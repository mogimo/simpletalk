package com.example.simpletalk;


import jp.aij.microaitalkjni.MicroAITalkJni;
import jp.aij.microaitalkjni.MicroAITalkJni.AIState;
import jp.aij.microaitalkjni.MicroAITalkJni.ErrCode;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * micro AITalk 3 サンプルソース(音声合成＆音声出力スレッド クラス)
 * @author 株式会社エーアイ
 */
public class StreamThreads {
	/*
	 * バッファサイズは大きければ大きいほどメモリリソースを食う
	 * 小さければ音が途切れる可能性がある。
	*/
	private final int BuffSize = 16000;/* バッファサイズ(とりあえず1sec分) */
	//private final int BuffSize = 8000;/* バッファサイズ(とりあえず0.5sec分) */
	
	private boolean err_flg = false;

	private int			stream_status = 0; //0:再生完了前 1:再生中 2:再生完了(MainActivity未確認) 3:再生完了(MainActivity確認済み)
	
	/* スレッド群 */
	/* 音声合成(AudioTrack)スレッド */
	private TtsAudioTrackThread ttsAudioTrackThread;
	/* 音声出力(AudioTrack)スレッド */
	private PlayAudioTrackThread playAudioTrackThread;
	
	//音声合成エンジン(MicroAITalkJniクラス) インスタンス取得
	MicroAITalkJni microaitalk = MicroAITalkJni.getInstance();

	// for singleton instance
	private static StreamThreads instance = new StreamThreads();

	private StreamThreads() {
	}

	public static StreamThreads getInstance() {
		return instance;
	}

	/* 音声合成スレッド(TTSを実行する)  */
	class TtsAudioTrackThread extends Thread {
		String inputTxt;
		public TtsAudioTrackThread(){
		}

		public void run(){
			try {
				err_flg = false;

				Log.v("MicroAITalkSDK:DEMO","Input TXT Size= "+ttsAudioTrackThread.inputTxt.getBytes("SJIS").length);

				/* 音声合成を開始する */
				ErrCode ret = microaitalk.start(ttsAudioTrackThread.inputTxt, BuffSize);
				if(ret != MicroAITalkJni.ErrCode.NONE){
					Log.v("MicroAITalkSDK:DEMO","Buff Start Error EC="+ret);
					err_flg = true;/* startメソッドでエラーを返していた場合 err_flgで音声出力スレッドに通知 */
				}
			} catch (Exception e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
				err_flg = true;
			}
		}
		/**
		 * 音声合成スレッド開始のためのメソッド
		 * @param inputVal 入力テキスト
		 * @throws Exception IllegalThreadStateException  if the Thread has been started before. <br>
		 * InterruptedException  if interrupt() was called for this Thread while it was sleeping.
		 */
		public void ttsStart(String inputVal) throws Exception {
			Thread.sleep(50);
			ttsAudioTrackThread.inputTxt = inputVal;
			ttsAudioTrackThread.start();
		}
	}

	/* 音声データ再生スレッド(音声データを取得して、音声デバイスに渡す。) */
	class PlayAudioTrackThread extends Thread {
		/**
		 * 音声データのサンプリングレート<br>
		 * AITalk音声:標準16kHz<br>
		 */
		private final int smplRate = 16000;
		private AudioTrack audioTrack;
		private int bufferSizeInBytes;

		public boolean stop_flg = true;
		public boolean pauseFlg = false;
		
		public PlayAudioTrackThread() {
			/* AudioTrackの設定はバッファサイズ */
			bufferSizeInBytes = AudioTrack.getMinBufferSize(smplRate,
					AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT);
            Log.v("PlayAudioTrackThread","miniBufferSize="+bufferSizeInBytes);
			bufferSizeInBytes = bufferSizeInBytes < smplRate * 2 * 2 ? smplRate * 2 * 2
					: bufferSizeInBytes;

			audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,		//streamType  the type of the audio stream. the type of the audio stream. See STREAM_VOICE_CALL, STREAM_SYSTEM, STREAM_RING, STREAM_MUSIC, STREAM_ALARM, and STREAM_NOTIFICATION.
										smplRate,						//sampleRateInHz:micro AITalk 3のサンプリングレートは16kHz
										AudioFormat.CHANNEL_OUT_MONO,	//channelConfig：micro AITalk 3の音声はモノラル
										AudioFormat.ENCODING_PCM_16BIT, //audioFormat :micro AITalk 3の音声は16bit PCM
										bufferSizeInBytes,				//bufferSizeInBytes:バッファサイズ(アプリ次第。最小値については、getMinBufferSize()で確認)
										AudioTrack.MODE_STREAM			//mode  streaming or static buffer. See MODE_STATIC and MODE_STREAM 
										);
			Log.v("PlayAudioTrackThread","bufferSizeInBytes="+bufferSizeInBytes);
	        audioTrack.setPlaybackPositionUpdateListener(
	                new AudioTrack.OnPlaybackPositionUpdateListener() {
	                    public void onPeriodicNotification(AudioTrack track) {
	                    	//Log出力のみ
	                        Log.v("audioTrack","onPeriodicNotification-----------------------------------");
	                    }
	                    // setNotificationMarkerPositionのコールバック
	                    public void onMarkerReached(AudioTrack track) {
	                        Log.v("audioTrack","onMarkerReached------------------------------------------");
	                        setStreamStatus(2);//2:再生完了(MainActivity未確認)
	                    }
	                }
	            );
	        audioTrack.setPositionNotificationPeriod(16000);//16000フレーム(16kHzなので1sec)ごとにonPeriodicNotificationを呼ぶ
		}
		
		public void run() {
			short buff[] = new short[BuffSize];/* ユーザバッファ */
			int size = 0;

			int tmpSize = 0;
			int sum = 0;
			
			stop_flg = false; //停止処理確認フラグ
			pauseFlg = false; //一時停止確認フラグ(サンプル未使用)
			Log.v("PlayAudioTrackThread","PlayAudioTrackThread Step1");
			
			/* TTS開始待ちループ */
			while (true){
				if(err_flg)break;/* startメソッドでエラーを返していた場合ブレーク */

				/* 音声合成エンジンの状態を確認し、PLAYINGもしくはPLAYENDの状態であることを確認する */
				AIState tmp_stat = microaitalk.getState();
				if( tmp_stat == MicroAITalkJni.AIState.BUFFERING
					||  tmp_stat == MicroAITalkJni.AIState.BUFFEND)
				{
					Log.v("PlayAudioTrackThread","PlayerMainLoop Start (stat="+tmp_stat+")");
					break;
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			boolean play_flg = false; //audioTrack.play();実行確認フラグ
			Log.v("PlayAudioTrackThread","PlayAudioTrackThread Step2");
			/* 音声出力ループ */
			while (true) {
				int writtenSize = 0;
				if(err_flg)break;/* startメソッドでエラーを返していた場合ブレーク */

				AIState tmp_stat = microaitalk.getState();
				if( tmp_stat != MicroAITalkJni.AIState.BUFFERING && tmp_stat != MicroAITalkJni.AIState.BUFFEND) break;

				writtenSize = 0;

				/* 音声合成エンジンの合成済みデータを取得する */
				size = microaitalk.checkData(buff);

				if ( size == MicroAITalkJni.ErrCode.convertFromJavaErrCode(MicroAITalkJni.ErrCode.NOMORETXT) /* no more txt */){
					/* checkDataメソッドの戻り値がNOMORETXTの場合は全入力テキストの合成＆データ取得は完了している  */
					if(!play_flg){
						/* audioTrack.play未実行であればaudioTrack.playを実行する */
						audioTrack.play();
						play_flg = true;
					}
					Log.v("PlayAudioTrackThread","NOTEXT break");
					break;
				}
				else if(size < 0){ /* Errorチェック */
					Log.e("PlayAudioTrackThread","checkData ERROR(ERR="+MicroAITalkJni.ErrCode.convertFromNativeErrCode(size)+")");

					tmp_stat = microaitalk.getState();
					if( tmp_stat != MicroAITalkJni.AIState.BUFFERING && tmp_stat != MicroAITalkJni.AIState.BUFFEND){
						err_flg = true;
						break;
					}
				}

				/* 音声データのaudioTrackへの書き込み */
				while (size - writtenSize > 0) {
					if(stop_flg)
						break;
					tmpSize = audioTrack.write(buff, writtenSize, size - writtenSize);
					Log.v("PlayAudioTrackThread","Play1 written size="+tmpSize+"----------");

					if (tmpSize == 0){
						break;
					}
					writtenSize += tmpSize;
				}

				sum += writtenSize;
				
				audioTrack.setNotificationMarkerPosition(sum);// コールバックするタイミングを指定
				Log.v("PlayAudioTrackThread","setNotificationMarkerPosition1 = "+sum+"----------");
				
				if (size > 0 && !stop_flg){
					/* audioTrackへ書き込み済みのデータサイズと同じサイズの内部バッファを解放する */
					ErrCode ret = microaitalk.release(writtenSize);
					/* Errorチェック */
					if(ret != MicroAITalkJni.ErrCode.NONE){
						Log.e("PlayAudioTrackThread","release ERROR(ERR="+ret+")");
						tmp_stat = microaitalk.getState();
						if( tmp_stat != MicroAITalkJni.AIState.BUFFERING && tmp_stat != MicroAITalkJni.AIState.BUFFEND){
							err_flg = true;
							break;
						}
					}
				}

				// スタート時のバッファリング
				if (sum >= bufferSizeInBytes/2 && play_flg == false) {
					audioTrack.play();
					play_flg = true;
				}

				/* 一時停止関連処理 */
				if(pauseFlg){
					audioTrack.pause(); /* オーディオ出力をポーズ */
					/* 一時停止ループ */
					while(pauseFlg){
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if(stop_flg){
							/* 一時停止中に停止を実行した場合はブレーク */
							pauseFlg = false;
							break;
						}
					}
					if(!stop_flg)
						audioTrack.play();/* オーディオ出力を再開 */
				}

				/* 停止関連処理 */
				if(stop_flg){
					audioTrack.pause();
					Log.v("PlayAudioTrackThread","STOP!!");
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}//while(true)
			
			Log.v("PlayAudioTrackThread","PlayAudioTrackThread Step3");
			
			if(!stop_flg){
			/* 音声データが短いと再生が不安定になるので空データをwriteする */
				if(sum < bufferSizeInBytes/2){
					short tmpbuf[] = new short[bufferSizeInBytes/2];
					audioTrack.write(tmpbuf, 0, bufferSizeInBytes/2);
					audioTrack.setNotificationMarkerPosition(tmpbuf.length);// コールバックするタイミングを指定
					audioTrack.play();
					Log.v("PlayAudioTrackThread","Play2  written size="+tmpbuf+"("+tmpSize+")----------");
					Log.v("PlayAudioTrackThread","setNotificationMarkerPosition2 = "+tmpbuf.length+"----------");
				}
			}

			while (true) {
				if(getStreamStatus() == 3)break;
				if(stop_flg) break;
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			Log.v("PlayAudioTrackThread","PlayAudioTrackThread Step4");
			//後始末
			buff = null;//
			audioTrack.stop();    //audioTrack停止
			audioTrack.release(); //audioTrackリリース
			setStreamStatus(0);
			
			if(err_flg)
			{
				Log.e("PlayAudioTrackThread","TTS ERROR");
			}
			stop_flg = true;
			err_flg = false;
		}

		/* 音声出力開始メソッド */
		public void Play() throws Exception {
			Thread.sleep(50);
			playAudioTrackThread.start();
		}
		/* 一時停止メソッド */
		public void pause(){
			pauseFlg = true;
		}
		/* 一時停止からの再再生メソッド */
		public void restart(){
			pauseFlg = false;
		}
	}
	
	public void StreamStart(String inputStr){
		try {
			setStreamStatus(1);
			/* 音声合成スレッドで作成した音声データを音声出力スレッドで取得し再生する。 */
			playAudioTrackThread = new PlayAudioTrackThread();
			playAudioTrackThread.Play();
			ttsAudioTrackThread = new TtsAudioTrackThread();
			ttsAudioTrackThread.ttsStart(inputStr);
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	/* 停止メソッド */
	public void stopStream(){
		if(playAudioTrackThread != null){
			playAudioTrackThread.stop_flg = true;
			Log.v("PlayAudioTrackThread","stopStream()");
		}
	}
	
	/* 状態情報取得メソッド */
	public int getStreamStatus(){
		return stream_status;
	}
	/* 状態情報設定メソッド */
	public void setStreamStatus(int val){
		stream_status = val;
	}
}
