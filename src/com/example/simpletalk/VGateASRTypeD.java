package com.example.simpletalk;

import android.os.Handler;
import android.util.Log;

import com.fuetrek.fsr.FSRService;
import com.fuetrek.fsr.FSRServiceEventListener;
import com.fuetrek.fsr.FSRServiceEnum.BackendType;
import com.fuetrek.fsr.FSRServiceEnum.EventType;
import com.fuetrek.fsr.FSRServiceEnum.IOMode;
import com.fuetrek.fsr.FSRServiceEnum.LogLevel;
import com.fuetrek.fsr.FSRServiceEnum.ResultState;
import com.fuetrek.fsr.FSRServiceEnum.State;
import com.fuetrek.fsr.entity.AbortInfoEntity;
import com.fuetrek.fsr.entity.CodecAssignEntity;
import com.fuetrek.fsr.entity.ConstructEntity;
import com.fuetrek.fsr.entity.DtypeInfoEntity;
import com.fuetrek.fsr.entity.IOSourceEntity;
import com.fuetrek.fsr.entity.IndividualEntity;
import com.fuetrek.fsr.entity.RecognizeEntity;
import com.fuetrek.fsr.entity.ResultInfoEntity;
import com.fuetrek.fsr.entity.StartParamEntity;
import com.fuetrek.fsr.entity.VoiceControlEntity;
import com.fuetrek.fsr.exception.AbortException;
import com.fuetrek.fsr.exception.FSRServiceException;
import com.fuetrek.fsr.exception.MemoryException;
import com.fuetrek.fsr.exception.NoDataException;
import com.fuetrek.fsr.exception.NoResourceException;
import com.fuetrek.fsr.exception.OperationException;
import com.fuetrek.fsr.exception.ParameterException;
import com.fuetrek.fsr.exception.TooManyDataException;

public class VGateASRTypeD implements Recognizer {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk:vGateASR";

    private final static String CODEC = "SPEEX";
    private final static int RECORD_SIZE = 240;
    private final static int SPEECH_TIME = 3500;
    private final static int RECOGNIZE_TIME = 6000;

    private final static BackendType BACKEND_DTYPE = BackendType.D;
    private final static String BACKEND_URL = "220.110.160.33/tetra-asp06/FSSP/";
    private final static String BACKEND_SERVICE = "test-domain";

    private FSRService mService;
    private FSRServiceEventListener mEventListener = new ServiceEventLister();

    private final static int RETRY_DURATION = 2000;
    private final static int RETRY_LIMIT = 5;
    private int retryCount = 0;
    private Handler mHandler = new Handler();
    private RecognizerListener mClient;

    private class ServiceEventLister implements FSRServiceEventListener {

        @Override
        public void notifyAbort(Object appHandle, AbortInfoEntity abortInfo) {
            Log.e(TAG, "unresolvable error!!=" + abortInfo);
            // TODO: do something
        }

        private void getSessionResult() {
            String result = null;
            float score = 0.0f;

            if (mService == null && mClient != null) {
                mClient.onRecognize(result, score);
            }
            RecognizeEntity entity;
            try {
                State state = mService.getStatus();
                if (DEBUG) Log.d(TAG, "session result state=" + state);
                if (state != State.READY) {
                    return;
                }
                entity = mService.getSessionResultStatus(BACKEND_DTYPE);
                ResultState ret = entity.getResultState();
                if (DEBUG) Log.d(TAG, "session result result=" + ret);
                if (ret != ResultState.NORMAL) {
                    switch (ret) {
                        case CANCEL:
                        case NODATA:
                            mClient.onError(Recognizer.ERROR_NO_MATCH);
                            break;
                        case TIMEOUT:
                            mClient.onError(Recognizer.ERROR_TIMEOUT);
                            break;
                        case ERROR:
                        default:
                            mClient.onError(Recognizer.ERROR_SERVER);
                            break;
                    }
                    return;
                }
                long count = entity.getCount();
                if (DEBUG) Log.d(TAG, "session result count=" + count);
                if (count > 0) {
                    ResultInfoEntity info = mService.getSessionResult(BACKEND_DTYPE, 1);
                    result = info.getText();
                    score = (float) info.getNumberInfo()/100.0f;
                }
                mClient.onRecognize(result, score);
                return;
            } catch (AbortException e) {
                e.printStackTrace();
            } catch (ParameterException e) {
                e.printStackTrace();
            } catch (OperationException e) {
                e.printStackTrace();
            } catch (NoDataException e) {
                e.printStackTrace();
            }
            if (mClient != null) {
                mClient.onError(Recognizer.ERROR_SERVER);
            }
        }

        private String getResult() {
            String result = null;
            try {
                result = mService.getProgressText();
                result = result.replace("/", "");
            }
            catch (FSRServiceException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        public void notifyEvent(Object appHandle, 
                EventType eventType, BackendType backendType, Object eventData) {
            if (DEBUG) Log.d(TAG, "notifyEvent type=" + eventType);
            switch(eventType){
                case CompleteConnect: // 接続完了
                    if (mClient != null) {
                        mClient.onReady();
                        startSession();
                    }
                    break;
                case CompleteDisconnect: // 切断完了
                    break;
                case NotifyEndRecognition: // 認識完了
                    getSessionResult();
                    //stop();
                    break;
                case NotifyResultProgress: //認識経過通知
                    if (mClient != null) {
                        mClient.onRecognize(getResult(), 1.0f);
                    }
                    break;
                case NotifyLevel: // レベルメータ更新
                    int level = (Integer)eventData;
                    if (DEBUG) Log.i(TAG, "level=" + level);
                    break;
                default:
                    break;
                }
        }
        
    }

    @Override
    public void init() {
        final IndividualEntity individual = new IndividualEntity();
        individual.setApplication("SHARP");
        individual.setTerminalId("SIM Serial");
        individual.setTerminalType("IMEI number");

        final CodecAssignEntity codec = new CodecAssignEntity();
        codec.setCodec(CODEC);
        codec.setRecordSize(RECORD_SIZE);

        final ConstructEntity construct = new ConstructEntity();
        construct.setIndividual(individual);
        construct.setCodecAssign(codec);
        construct.setSpeechTime(SPEECH_TIME);
        construct.setRecognizeTime(RECOGNIZE_TIME);
        construct.setLogLevel(LogLevel.None);
        construct.setRapidMode(false);

        if (null == mService){
            try {
                mService = new FSRService(mEventListener, mEventListener, construct);
            } catch (MemoryException e) {
                e.printStackTrace();
            } catch (NoResourceException e) {
                e.printStackTrace();
            } catch (ParameterException e) {
                e.printStackTrace();
            } catch (AbortException e) {
                e.printStackTrace();
            }
        }
        connect();

        // wait CompleteConnect event notify
    }

    private void connect() {
        if (mService != null) {
            try {
                DtypeInfoEntity dtypeInfo = new DtypeInfoEntity();
                dtypeInfo.setBackend(BACKEND_URL);
                dtypeInfo.setPortNo(80);
                dtypeInfo.setConnectLimit(10000);
                mService.connectSession(BACKEND_DTYPE, dtypeInfo);
            } catch (AbortException e) {
                e.printStackTrace();
            } catch (ParameterException e) {
                e.printStackTrace();
            } catch (OperationException e) {
                e.printStackTrace();
            }
        }
    }

    private void retrySessionStart() {
        retryCount++;
        if (retryCount < RETRY_LIMIT) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startSession();
                }
            }, RETRY_DURATION);
        } else {
            if (mClient != null) {
                mClient.onError(Recognizer.ERROR_TIMEOUT);
            }
        }
    }

    private void startSession() {
        if (mService != null) {
            State state = mService.getStatus();
            if (DEBUG) Log.d(TAG, "startSession() state=" + state);
            if (state == State.WAIT) {
                retrySessionStart();
                return;
            } else if (state == State.READY){
                try {
                    IOSourceEntity ioSource = new IOSourceEntity();
                    mService.setIOSource(IOMode.ModePcmMic, ioSource);

                    final VoiceControlEntity voiceControl = new VoiceControlEntity();
                    voiceControl.setAutoStart(true);
                    voiceControl.setAutoStop(true);
                    voiceControl.setVadOffTime(500);
                    voiceControl.setVadSensibility(0);
                    voiceControl.setListenTime(0);
                    voiceControl.setLevelSensibility(10);

                    final StartParamEntity startParam = new StartParamEntity();
                    startParam.setDTypeService(BACKEND_SERVICE);
                    startParam.setDTypeResultProgress(false);  // 当該サーバー非対応

                    mService.startRecognition(BACKEND_DTYPE, voiceControl, startParam);

                    state = mService.getStatus();
                    if (DEBUG) Log.d(TAG, "called startRecognition() state=" + state);
                } catch (AbortException e) {
                    e.printStackTrace();
                } catch (ParameterException e) {
                    e.printStackTrace();
                } catch (OperationException e) {
                    e.printStackTrace();
                } catch (NoResourceException e) {
                    e.printStackTrace();
                } catch (TooManyDataException e) {
                    e.printStackTrace();
                } catch (NoDataException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void start() {
        startSession();
    }

    @Override
    public void stop() {
        try {
            State state = mService.getStatus();
            if (DEBUG) Log.d(TAG, "stop() state=" + state);
            if (mService != null && state != State.IDLE) {
                //mService.stopRecognition();
                if (DEBUG) Log.d(TAG, "call disconnectSession() state=" + state);
                mService.disconnectSession(BACKEND_DTYPE);
            }
        } catch (AbortException e) {
            e.printStackTrace();
        } catch (ParameterException e) {
            e.printStackTrace();
        } catch (OperationException e) {
            e.printStackTrace();
        } catch (NoResourceException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void release() {
        if (mService != null) {
            mService.destroy();
            mService = null;
        }
        if (mClient != null) {
            mClient = null;
        }

    }

    @Override
    public void setRecognizerListener(RecognizerListener listener) {
        if (mClient == null) {
            mClient = listener;
        } else {
            Log.i(TAG, "already registered a listner");
        }
    }

}
