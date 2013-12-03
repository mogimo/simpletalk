package jp.aij.microaitalkjni;

import java.io.UnsupportedEncodingException;

import android.util.Log;

//import android.util.Log;

/**
 * micro AITalk 3 サンプルソース(JNIクラス)
 * @author 株式会社エーアイ
 *
 */
public class MicroAITalkJni {

		//共有ライブラリのロード
		static {
			try{
				System.loadLibrary("gnustl_shared");//STLライブラリ(NDK)
			} catch (UnsatisfiedLinkError use) {
				Log.e("JNI", "WARNING: Could not load libgnustl_shared.so");
			}
			try{
				System.loadLibrary("maitalk");//音声合成ライブラリ(micro AITalk)
			} catch (UnsatisfiedLinkError use) {
				Log.e("JNI", "WARNING: Could not load libmaitalk.so");
			}
			try{
				System.loadLibrary("maitalkjni");//JNIライブラリ(サンプル)
			} catch (UnsatisfiedLinkError use) {
				Log.e("JNI", "WARNING: Could not load libmaitalkjni.so");
			}
		}
		// from enum mAITalkErrCode
		/**
		 * micro AITalk SDK(Android版)APIメソッドの返す値(エラーコード)を持つ列挙型クラス
		 */
		public enum ErrCode {
			/** エラーなし  */
			NONE,
			/** 未初期化 */
			NOTINI,
			/** 各種辞書がロードされていません */
			NOLOADDIC,
			/** 言語処理辞書がロードされていません */
			NOLANGDIC,
			/** ユーザ単語辞書がロードされていません */
			NOUDIC,	
			/** 初期化済み */
			ALREADYINIT,
			/** ロード済みです。 */
			ALREADYLOAD,
			/** 入力テキストデータ無し */
			NO_INPUT,
			/** パラメタエラー */
			INPUT_PARAM,
			/** 合成処理中です */
			ENGINE_BUSY,
			/** 合成処理は実行中でありません */
			ENGINE_IDLE,
			/** 各種辞書処理中 */
			DIC_BUSY,
			
			/** バッファに合成データがありません。合成完了 */
			NOMORETXT,
			
			/** ライセンスエラー */
			LICENSE1,
			LICENSE2,
			LICENSE3,
			LICENSE4,
			
			/** 内部エラー */
			INTERNALERR1,
			INTERNALERR2,
			
			/** 非サポート機能です。 */
			NOTSUPPORT;

			/**
			 * JNIメソッドが返す値(int型)をエラーコード(ErrCode型)に変換するメソッド
			 * @param nativeErrCode JNIで使用するエラーコード値
			 * @return エラーコード
			 *         (nativeErrCodeの値が範囲外であった場合はnullを返します。)
			 */
			public static ErrCode convertFromNativeErrCode(int nativeErrCode) {
				ErrCode ret = null;
				switch (nativeErrCode) {
				case 0:
					ret = NONE;
					break;
				case -1:
					ret = NOTINI;
					break;
				case -2:
					ret = NOLOADDIC;
					break;
				case -3:
					ret = NOLANGDIC;
					break;
				case -4:
					ret = NOUDIC;
					break;
				case -5:
					ret = ALREADYINIT;
					break;
				case -6:
					ret = ALREADYLOAD;
					break;
				case -7:
					ret = NO_INPUT;
					break;
				case -8:
					ret = INPUT_PARAM;
					break;
				case -9:
					ret = ENGINE_BUSY;
					break;
				case -10:
					ret = ENGINE_IDLE;
					break;
				case -11:
					ret = DIC_BUSY;
					break;
					
				case -100:
					ret = NOMORETXT;
					break;
					
				case -200:
					ret = LICENSE1;
					break;
				case -201:
					ret = LICENSE2;
					break;
				case -202:
					ret = LICENSE3;
					break;
				case -203:
					ret = LICENSE4;
					break;
					
				case -300:
					ret = INTERNALERR1;
					break;
				case -301:
					ret = INTERNALERR2;
					break;
				case -400:
					ret = NOTSUPPORT;
					break;
				}
				return ret;
			}
			/**
			 * エラーコード(ErrCode型)をJNIメソッドが返す値(int型)に変換するメソッド
			 * @param errcode : エラーコード
			 * @return JNIで使用するエラーコード値
			 *         (errcodeが範囲外であった場合、1を返します。)
			 */
			public static int convertFromJavaErrCode(ErrCode errcode) {
				int ret = 0;
				switch (errcode) {
				case NONE:
					ret = 0;
					break;
				case NOTINI:
					ret = -1;
					break;
				case NOLOADDIC:
					ret = -2;
					break;
				case NOLANGDIC:
					ret = -3;
					break;
				case NOUDIC:
					ret = -4;
					break;
				case ALREADYINIT:
					ret = -5;
					break;
				case ALREADYLOAD:
					ret = -6;
					break;
				case NO_INPUT:
					ret = -7;
					break;
				case INPUT_PARAM:
					ret = -8;
					break;
				case ENGINE_BUSY:
					ret = -9;
					break;
				case ENGINE_IDLE:
					ret = -10;
					break;
				case DIC_BUSY:
					ret = -11;
					break;
					
				case NOMORETXT:
					ret = -100;
					break;
					
				case LICENSE1:
					ret = -200;
					break;
				case LICENSE2:
					ret = -201;
					break;
				case LICENSE3:
					ret = -202;
					break;
				case LICENSE4:
					ret = -203;
					break;
					
				case INTERNALERR1:
					ret = -300;
					break;
				case INTERNALERR2:
					ret = -301;
					break;
					
				case NOTSUPPORT:
					ret = -400;
					break;
				default:
					/* 入力値がmicroAITalkSDKで使用するエラーコード以外 */
					ret = 1;
					break;
				}
				return ret;
			}
		};

		// from mAITalkStatus
		/**
		 * 音声合成エンジンの動作状態を持つ列挙型クラス
		 * @see MicroAITalkJni#getState()
		 */
		public enum AIState {
			/** 未初期化状態 */
			NOTINIT,
			/** 初期化完了後、音声辞書/言語処理辞書ともにロードしていない状態 */
			NOLOADDIC,
			/** 言語処理辞書のみロードし、音声辞書をロードしていない状態 */
			NOVOICE,
			/** InputModeが日本語入力設定で、音声辞書はロードし、言語処理辞書をロードしていない状態 */
			NOLANG,
			/**
			 * 音声合成処理開始待ち状態。(次の何れか)<br>
			 * (1)InputModeが日本語入力設定で、音声DB/言語処理DBともロードされている。<br>
			 * (2)InputModeが日本語入力以外の設定で、音声DBがロードされている。<br>
			 */
			IDLE,
			/** 音声合成処理中 */
			BUFFERING,
			/** 音声合成完了、バッファ処理完了待ち状態 */
			BUFFEND,
			/** 音声合成処理中、ファイル出力中 */
			FILE,
			/** 日本語解析、カナ出力中 */
			KANA;

			/**
			 * JNIで使用するステイト値をStateに変換するメソッド
			 * @param nativeState JNIで使用するステイタス値
			 * @return State
			 *         (nativeStateの値が範囲外であった場合はnullを返します。)
			 */
			private static AIState convertFromNativeState(int nativeState) {
				AIState ret = null;
				switch (nativeState) {
				case 0:
					ret = NOTINIT;
					break;
				case 1:
					ret = NOLOADDIC;
					break;
				case 2:
					ret = NOVOICE;
					break;
				case 3:
					ret = NOLANG;
					break;
				case 4:
					ret = IDLE;
					break;
				case 5:
					ret = BUFFERING;
					break;
				case 6:
					ret = BUFFEND;
					break;
				case 7:
					ret= FILE;
					break;
				case 8:
					ret= KANA;
				}
				return ret;
			}

			/**
			 * StatusをJNIで使用するステイタス値に変換するメソッド
			 * @param state State
			 * @return JNIで使用するステイタス値
			 *         (statusが範囲外であった場合、-1を返します。)
			 */
			@SuppressWarnings("unused")
			private static int convertFromJavaStatus(AIState state) {
				int ret = -1;
				switch(state){
				case NOTINIT:
					ret = 0;
					break;
				case NOLOADDIC:
					ret = 1;
					break;
				case NOVOICE:
					ret = 2;
					break;
				case NOLANG:
					ret = 3;
					break;
				case IDLE:
					ret = 4;
					break;
				case BUFFERING:
					ret = 5;
					break;
				case BUFFEND:
					ret = 6;
					break;
				case FILE:
					ret = 7;
					break;
				case KANA:
					ret = 8;
					break;
				}
				return ret;
			}
		}

		// for singleton instance
		/**
		 *  MicroAITalkSDKのインスタンス
		 */
		private static MicroAITalkJni instance = new MicroAITalkJni();

		/**
		 * コンストラクタ
		 */
		private MicroAITalkJni() {

		}

		/**
		 * MicroAITalkSDKのインスタンスを返すメソッド
		 * @return  音声合成エンジン(MicroAITalkSDK) インスタンス
		 */
		public static MicroAITalkJni getInstance() {
			return instance;
		}

		/* java I/F  */
		/* normal */
		// Common API
		// mAITalkErrCode mAITalk_GetVersionInfo( char* version, int len);
		/**
		 * バージョン情報を取得するためのメソッド<br>
		 * micro AITalk SDK のバージョン情報を取得するメソッド。
		 * @return バージョン情報(String型)
		 */
		public String getVersionInfo() {
			return JNIGetVersionInfo();
		}

		// mAITalkErrCode mAITalk_GetState( mAITalkState* state );
		/**
		 * 音声合成エンジンの動作状態を取得するメソッド。<br>
		 * @return ステイト値<br>
		 * NOTINIT: 未初期化状態<br>
		 * NOLOADDB: 初期化完了後、音声DB/言語処理DBともにロードしていない状態<br>
		 * NOVOICE: 言語処理DBのみロードし、音声DBをロードしていない状態<br>
		 * NOLANG: InputModeが日本語入力設定で、音声DBはロードし、言語処理DBをロードしていない状態<br>
		 * IDLE: 音声合成処理開始待ち状態<br>
		 * (1)InputModeが日本語入力設定で、音声DB/言語処理DBともロードされている。<br>
		 * (2)InputModeが日本語入力以外の設定で、音声DBがロードされている。<br>
		 * BUFFRING: 音声合成処理中<br>
		 * BUFFEND: 音声合成完了、バッファ処理完了待ち状態<br>
		 * KANA: AIカナ出力処理中
		 * @see Status
		 */
		public AIState getState() {
			return AIState.convertFromNativeState(JNIGetStateInfo());
		}

		// mAITalkErrCode mAITalk_Init( const char* voicePath, const char* licPath, const char* licKey );
		/**
		 * 音声合成エンジン初期化メソッド。<br>
		 * micro AITalk SDK の各種API は、init()での初期化後に利用可能となります。<br>
		 * 引数で各種DBおよび設定ファイルの格納先ディレクトリ(DBパス)を指定します。
		 * @return エラーコード<br>
		 *  NONE : 正常終了<br>
		 *  INOUT_PARAM : パラメタエラー<br>
		 *  INTERNALERR : 内部エラー<br>
		 *  ALREADYINIT : 初期化済み<br>
		 * @see ErrCode
		 * @see Status#NOTINIT
		 */
		public ErrCode init()  {
			// TODO: CONVERT TO SJIS
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNIInit());

//			if (errCode != ErrCode.NONE) {
//				/* TODO */
//				Log.w("microAITalkSDK","microAITalkAndroid  Init Error=" + errCode);
//			}
			return errCode;
		}

		// mAITalkErrCode mAITalk_LangLoad( const char* langPath);
		/**
		 * 言語辞書をロードするためのメソッド。
		 * @param langType 辞書の種類
		 * @return  エラーコード<br>
		 *  NONE : 正常終了<br>
		 *  NOTINI : 未初期化<br>
		 *  ENGINE_BUSY : 音声合成処理中<br>
		 *  INTERNALERR : 内部エラー<br>
		 *  ALREADYSET : ロード済み<br>
		 *  NOTSUPPORT : 言語処理機能非搭載の場合<br>
		 *  @see ErrCode
		 *  @see Status#NOLOADDB
		 *  @see Status#NOLANG
		 */
		public ErrCode loadLang(boolean langType )  {
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNILangLoad(langType));

//			if (errCode != ErrCode.NONE) {
//				/* TODO */
//				Log.w("microAITalkSDK","microAITalkAndroid  LangLoad Error=" + errCode);
//			}
			return errCode;
		}
		// mAITalkErrCode mAITalk_UdicLoad( const char* udicPath);
		/**
		 * 言語辞書をロードするためのメソッド。
		 * @return  エラーコード<br>
		 *  NONE : 正常終了<br>
		 *  NOTINI : 未初期化<br>
		 *  ENGINE_BUSY : 音声合成処理中<br>
		 *  INTERNALERR : 内部エラー<br>
		 *  ALREADYSET : ロード済み<br>
		 *  NOTSUPPORT : 言語処理機能非搭載の場合<br>
		 *  @see ErrCode
		 *  @see Status#NOLOADDB
		 *  @see Status#NOLANG
		 */
		public ErrCode loadUdic()  {
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNIUdicLoad());

//			if (errCode != ErrCode.NONE) {
//				/* TODO */
//				Log.w("microAITalkSDK","microAITalkAndroid  LangLoad Error=" + errCode);
//			}
			return errCode;
		}
		// mAITalkErrCode mAITalk_VoiceLoad( const char* voiceName );
		/**
		 * 音声辞書をロードするためのメソッド。<br>
		 * init()で指定済みの辞書パス配下の話者名を引数で指定し、ロードすることができます。
		 * @param VoiceName 話者名
		 * @return エラーコード<br>
		 * NONE : 正常終了<br>
		 * INOUT_PARAM : パラメタエラー<br>
		 * NOTINI : 未初期化<br>
		 * ENGINE_BUSY : 音声合成処理中<br>
		 * INTERNALERR : 内部エラー<br>
		 * @see ErrCode
		 * @see Status#NOLOADDB
		 * @see Status#NOVOICE
		 */
		public ErrCode loadVoice(String VoiceName)  {
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNIVoiceLoad(VoiceName));

//			if (errCode != ErrCode.NONE) {
//				/* TODO */
//				Log.w("microAITalkSDK","microAITalkAndroid  VoiceLoad Error=" + errCode);
//			}
			return errCode;
		}

/* TODO パラメタ関連メソッド  */
		/* PARAM SET */
		//mAITalkErrCode mAITalk_SetParam( mAITalk_TtsParam *param );
		/**
		 * 音量を設定します。
		 * @param volume
		 * @return エラーコード
		 */
		public ErrCode setParamVolume(float Volume){
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNISetParamVolume(Volume));
			return errCode;
		}

		/**
		 * 話速を設定します。
		 * @param speed
		 * @return エラーコード
		 */
		public ErrCode setParamSpeed(float Speed){
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNISetParamSpeed(Speed));
			return errCode;
		}

		/**
		 * ピッチを設定します。
		 * @param pitch
		 * @return エラーコード
		 */
		public ErrCode setParamPitch(float Pitch){
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNISetParamPitch(Pitch));
			return errCode;
		}

		/**
		 * アクセント/抑揚の強さを設定します。
		 * @param emph
		 * @return エラーコード
		 */
		public ErrCode setParamEmph(float Emph){
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNISetParamEmph(Emph));
			return errCode;
		}

		/**
		 * 入力テキストモードを設定します。
		 * @param InputMode
		 * @return エラーコード
		 */
		public ErrCode setParamInputMode(int InputMode){
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNISetParamInputMode(InputMode));
			return errCode;
		}

		/**
		 * 中ポーズを設定します。
		 * @param PauseMiddle
		 * @return エラーコード
		 */
		public ErrCode setParamPauseMiddle(int PauseMiddle){
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNISetParamPauseMiddle(PauseMiddle));
			return errCode;
		}

		/**
		 * 長ポーズを設定します。
		 * @param PauseLong
		 * @return エラーコード
		 */
		public ErrCode setParamPauseLong(int PauseLong){
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNISetParamPauseLong(PauseLong));
			return errCode;
		}

		/**
		 * 文末ポーズを設定します。
		 * @param PauseSentence
		 * @return エラーコード
		 */
		public ErrCode setParamPauseSentence(int PauseSentence){
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNISetParamPauseSentence(PauseSentence));
			return errCode;
		}

		/**
		 * 話者を設定します。
		 * @param VoiceName
		 * @return エラーコード
		 */
		public ErrCode setParamVoiceName(String VoiceName){
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNISetParamVoiceName(VoiceName));
			return errCode;
		}

		/**
		 * JEITA女性話者を設定します。
		 * @param JeitaFemaleName
		 * @return エラーコード
		 */
		public ErrCode setParamJeitaFemaleName(String JeitaFemaleName){
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNISetParamJeitaFemaleName(JeitaFemaleName));
			return errCode;
		}

		/**
		 * JEITA男性話者を設定します。
		 * @param JeitaMaleName
		 * @return エラーコード
		 */
		public ErrCode setParamJeitaMaleName(String JeitaMaleName){
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNISetParamJeitaMaleName(JeitaMaleName));
			return errCode;
		}

		/* PARAM GET */
		//mAITalkErrCode mAITalk_GetParam( mAITalk_TtsParam *param );
		/**
		 * 音量を取得します。
		 * @return 音量またはエラー値
		 */
		public float getParamVolume(){
			/* paramに対応するパラメタが、数値データであることのチェックはJNIで行う  */
			float ret_num = 0;
			ret_num = JNIGetParamVolume( );
			return ret_num;
		}

		/**
		 * 話速を取得します。
		 * @return 話速またはエラー値
		 */
		public float getParamSpeed(){
			/* paramに対応するパラメタが、数値データであることのチェックはJNIで行う  */
			float ret_num = 0;
			ret_num = JNIGetParamSpeed( );
			return ret_num;
		}

		/**
		 * ピッチを取得します。
		 * @return ピッチまたはエラー値
		 */
		public float getParamPitch(){
			/* paramに対応するパラメタが、数値データであることのチェックはJNIで行う  */
			float ret_num = 0;
			ret_num = JNIGetParamPitch( );
			return ret_num;
		}


		/**
		 * アクセント/抑揚の強さの取得
		 * @return アクセント/抑揚の強さまたはエラー情報
		 */
		public float getParamEmph(){
			/* paramに対応するパラメタが、数値データであることのチェックはJNIで行う  */
			float ret_num = 0;
			ret_num = JNIGetParamEmph( );
			return ret_num;
		}

		/**
		 * 入力テキストモードの取得
		 * @return 入力テキストモードまたはエラー情報
		 */
		public int getParamInputMode(){
			/* paramに対応するパラメタが、数値データであることのチェックはJNIで行う  */
			int ret_num = 0;
			ret_num = JNIGetParamInputMode( );
			return ret_num;
		}

		/**
		 * 中ポーズの取得
		 * @return 中ポーズ長またはエラー情報
		 */
		public int getParamPauseMiddle(){
			/* paramに対応するパラメタが、数値データであることのチェックはJNIで行う  */
			int ret_num = 0;
			ret_num = JNIGetParamPauseMiddle( );
			return ret_num;
		}

		/**
		 * 長ポーズの取得
		 * @return 長ポーズまたはエラー情報
		 */
		public int getParamPauseLong(){
			/* paramに対応するパラメタが、数値データであることのチェックはJNIで行う  */
			int ret_num = 0;
			ret_num = JNIGetParamPauseLong( );
			return ret_num;
		}

		/**
		 * 文末ポーズの取得
		 * @return 文末ポーズまたはエラー情報
		 */
		public int getParamPauseSentence(){
			/* paramに対応するパラメタが、数値データであることのチェックはJNIで行う  */
			int ret_num = 0;
			ret_num = JNIGetParamPauseSentence( );
			return ret_num;
		}

		/**
		 * 話者名の取得
		 * @return 話者名またはエラー情報
		 */
		public String getParamVoiceName(  ){
			String ret_val;
			ret_val = JNIGetParamVoiceName();
			return ret_val;
		}

		/**
		 * JEITA女性話者名の取得
		 * @return JEITA女性話者名またはエラー情報
		 */
		public String getParamJeitaFemaleName(  ){
			String ret_val;
			ret_val = JNIGetParamJeitaFemaleName();
			return ret_val;
		}

		/**
		 * JEITA男性話者名の取得
		 * @return JEITA男性話者名またはエラー情報
		 */
		public String getParamJeitaMaleName(  ){
			String ret_val;
			ret_val = JNIGetParamJeitaMaleName();
			return ret_val;
		}

		// mAITalkErrCode mAITalk_VoiceUnload( );
		/**
		 * 音声辞書をアンロードします。
		 * @return エラーコード
		 * @see ErrCode
		 * @see Status
		 */
		public ErrCode unloadVoice(){
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNIVoiceUnload());
			return errCode;
		}

		// mAITalkErrCode mAITalk_LangUnload( );
		/**
		 * 言語処理辞書をアンロードします。
		 * @return エラーコード
		 * @see ErrCode
		 * @see Status
		 */
		public ErrCode unloadLang(){
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNILangUnload());
			return errCode;
		}

		// mAITalkErrCode mAITalk_UdicUnload( );
		/**
		 * ユーザ単語辞書をアンロードします。
		 * @return エラーコード
		 * @see ErrCode
		 * @see Status
		 */
		public ErrCode unloadUdic(){
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNIUdicUnload());
			return errCode;
		}
		
		// mAITalkErrCode mAITalk_End( );
		/**
		 * 終了メソッド。実行中の合成処理を停止させ、ロード済み各種辞書を解放します。<br>
		 * 本メソッド実行後、再度音声合成エンジンを使用するには、再度初期化してください。
		 * @return ErrCode :エラーコード<br>
		 * NONE : 正常終了<br>
		 * NOTINI : 未初期化<br>
		 * ENGINE_BUSY : 音声合成処理中<br>
		 * INTERNALERR : 内部エラー<br>
		 * @see ErrCode
		 * @see Status
		 */
		public ErrCode end(){
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNIEnd());

//			if (errCode != ErrCode.NONE) {
//				/* TODO */
//				Log.w("microAITalkSDK","microAITalkAndroid  End Error=" + errCode);
//			}
			return errCode;
		}

		/* PlayToFile */
		// mAITalkErrCode mAITalk_Player_PlayToFile( const char* inputText, const char* outputFileName, bool lipsyncOn ) ;
		/**
		 * 音声ファイル出力のためのメソッド。<br>
		 * inputText で入力されたテキストを音声合成し、FileName で指定した音声ファイルに保存します。<br>
		 * 処理中はPLAYING状態になり、終了後はIDLE状態になります。
		 * @param inputText 入力テキスト
		 * @param FileName 出力音声ファイル名(パスも含めて指定できます。また「.wav」拡張子は自動的に添付されます)
		 * @param リップシンクデータ出力ON/OFFフラグ (ON:true, OFF:false)(リップシンクデータはFileNameに「.lab」拡張子が添付されます)
		 * @return エラーコード<br>
		 * NONE : 正常終了<br>
		 * INPUT_PARAM : パラメタエラー<br>
		 * NOTINI : 未初期化<br>
		 * NOLOADDB : 音声DBが未ロードです。<br>
		 * NO_INPUT : テキスト入力がありません<br>
		 * ENGINE_BUSY : 音声合成処理中です<br>
		 * INTERNALERR : 内部エラー<br>
		 * NOTSUPPORT : 非サポート<br>
		 * NOLANGDB : 言語処理DBが未ロードです<br>
		 *
		 * @throws UnsupportedEncodingException 指定された文字セットがサポートされていない場合
		 * @see ErrCode
		 * @see Status
		 */
		public ErrCode playToFile(String inputText, String FileName,boolean lipSyncSwitch) throws UnsupportedEncodingException {
			ErrCode errCode;

			if(inputText.length() == 0){
				errCode = MicroAITalkJni.ErrCode.NO_INPUT;
			}
			else{
				errCode = ErrCode.convertFromNativeErrCode(JNIPlayToFile(inputText.getBytes("SJIS"), FileName, lipSyncSwitch));
			}
//			if (errCode != ErrCode.NONE) {
//				/* TODO */
//				Log.w("microAITalkSDK","microAITalkAndroid  PlayToFile Error=" + errCode);
//			}
			return errCode;
		}


		/* buffering */
		// mAITalkErrCode mAITalk_Buffering_Start(const char* inputText,short* usrBuf, int bufLen);
		/**
		 * 音声合成を開始するためのメソッド。<br>
		 * bufSize で指定したサイズの内部バッファを作成し、inputText で入力されたテキストを音声合成し音声データを内部バッファへ格納します。<br>
		 * 処理中はPLAYING状態になり、終了後はPLAYEND状態になります。
		 * @param inputText 入力テキスト
		 * @param buffSize 音声データ出力先となる内部バッファのサイズを指定します。
		 * @return エラーコード<br>
		 * NONE : 正常終了<br>
		 * INPUT_PARAM : パラメタエラー<br>
		 * NOTINI : 未初期化<br>
		 * NOLOADDB : 音声もしくは言語処理DBが未ロードです。<br>
		 * NO_INPUT : テキスト入力がありません<br>
		 * ENGINE_BUSY : 音声合成処理中です<br>
		 * INTERNALERR : 内部エラー<br>
		 * NOTSUPPORT : 非サポート<br>
		 * NOLANGDB : 言語処理DBが未ロードです<br>
		 *
		 * @throws UnsupportedEncodingException 指定された文字セットがサポートされていない場合
		 * @see ErrCode
		 * @see Status
		 * @see MicroAITalkJni#checkData(short[])
		 * @see MicroAITalkJni#release(int)
		 */
		public ErrCode start(String inputText, int buffSize) throws UnsupportedEncodingException {
			ErrCode errCode;

			if(buffSize < 0){
				errCode = MicroAITalkJni.ErrCode.INPUT_PARAM;
			}
			else if(inputText.length() == 0){
				errCode = MicroAITalkJni.ErrCode.NO_INPUT;
			}
			else{
				errCode = ErrCode.convertFromNativeErrCode(JNIBuffStart(inputText.getBytes("SJIS"), buffSize));
			}

//			if (errCode != ErrCode.NONE) {
//				/* TODO */
//				Log.w("microAITalkSDK","microAITalkAndroid  Start Error=" + errCode);
//			}
			return errCode;
		}

		// mAITalkErrCode mAITalk_Buffering_SetFilledCallback( void (*filledCallback)(void) );
		public ErrCode setFilledCallback(boolean cbSet){
			ErrCode errCode;

			errCode = ErrCode.convertFromNativeErrCode(JNIBuffSetFilledCallBack(cbSet));

			return errCode;
		}
		// mAITalkErrCode mAITalk_Buffering_SetLipSyncCallback( void (*lipsyncCallback)(unsigned long long, const char*,bool) );
		public ErrCode setLipsyncCallback(boolean cbSet){
			ErrCode errCode;

			errCode = ErrCode.convertFromNativeErrCode(JNIBuffSetLipsyncCallBack(cbSet));

			return errCode;
		}
		// mAITalkErrCode mAITalk_Buffering_SetSentenceCallback( void (*sentenceCallback)(const char*) );
		public ErrCode setSentenceCallback(boolean cbSet){
			ErrCode errCode;

			errCode = ErrCode.convertFromNativeErrCode(JNIBuffSetSentenceCallBack(cbSet));

			return errCode;
		}
		// mAITalkErrCode mAITalk_Buffering_SetBookMarkCallback	( void (*bookmarkCallback)(unsigned long long, const char*,bool) );
		public ErrCode setBookMarkCallback(boolean cbSet){
			ErrCode errCode;

			errCode = ErrCode.convertFromNativeErrCode(JNIBuffSetBookMarkCallBack(cbSet));

			return errCode;
		}
		
		// mAITalkErrCode mAITalk_Buffering_CheckData(int* getRp, int* getLen  );
		/**
		 * バッファデータの取得するためのメソッド。<br>
		 * start()メソッドで開始した音声合成処理で生成された音声データを取得することができます。<br>
		 * 音声合成が終了しバッファ内すべてのデータが解放されていた場合、NOMORETXT を返すことでstart()で開始した合成処理が終了したことを通知します。<br>
		 * NOMORETXT を返すとPLAYEND状態からIDLE状態に遷移します。
		 * @param buff 内部バッファのデータをコピーする先のバッファ
		 * @return データサイズ(short型配列の長さ)<br>
		 *  0未満の場合はエラーコードとなる。(下記エラー一覧参照)<br>
		 *  -3 : NOTINI :未初期化<br>
		 *  -4 : NOLOADDB :音声DB未ロード<br>
		 * -11 : NOMORETXT :音声合成処理終了のシグナル(エラーでは無い)<br>
		 * -13 : NOTSUPPORT :非サポート<br>
		 * -15 : NOTPLAYING : 音声合成処理が実行されていません<br>
		 * @see ErrCode
		 * @see ErrCode#convertFromNativeErrCode(int)
		 * @see Status#PLAYING
		 * @see Status#PLAYEND
		 * @see MicroAITalkJni#release(int)
		 * 		 */
		public int checkData(short[] buff){
			int ret_val;
			ret_val = JNIBuffCheckData(buff);

//			if(ret_val < 0 && ret_val != ErrCode.convertFromJavaErrCode(ErrCode.NOMORETXT)){
//				ErrCode errCode;
//				errCode = ErrCode.convertFromNativeErrCode(ret_val);
//				Log.w("microAITalkSDK","microAITalkAndroid  checkData Error=" + errCode);
//			}
			return ret_val;
		}

		// mAITalkErrCode mAITalk_Buffering_Release(int dataLen );
		/**
		 * 内部バッファ内のデータを解放するためのメソッド。<br>
		 * 内部バッファ内のデータを解放することで、新たな音声データをバッファに書き込ませることができます。<br>
		 * checkData()で取得したデータサイズ以上は解放しないでください、バッファ内の解放された領域には新たな音声データが上書きされます。
		 * @param datalen : 解放するデータサイズ
		 * @return エラーコード<br>
		 * NONE : 正常終了<br>
		 * INPUT_PARAM : パラメタエラー<br>
		 * NOTINI : 未初期化<br>
		 * NOLOADDB : 音声DB未ロード<br>
		 * NOTSUPPORT : 非サポート<br>
		 * NOTPLAYING : 音声合成処理が実行されていません<br>
		 * @see ErrCode
		 * @see Status#PLAYING
		 * @see MicroAITalkJni#checkData(short[])
		 */
		public ErrCode release(int datalen) {
			ErrCode errCode;
			if(datalen < 0){
				errCode = MicroAITalkJni.ErrCode.INPUT_PARAM;
			}
			else{
				errCode = ErrCode.convertFromNativeErrCode(JNIBuffRelease(datalen));
			}
//			if (errCode != ErrCode.NONE) {
//				/* TODO */
//				Log.w("microAITalkSDK","microAITalkAndroid  Release Error=" + errCode);
//			}
			return errCode;
		}

		// mAITalkErrCode mAITalk_Buffering_Stop( );
		/**
		 * 合成処理を停止するためのメソッド。<br>
		 * start()で開始した合成処理を停止させることができます。
		 * @return エラーコード <br>
		 * NONE : 正常終了<br>
		 * NOTINI : 未初期化<br>
		 * NOLOADDB : 音声DB未ロード<br>
		 * NOTSUPPORT : 非サポート<br>
		 * NOTPLAYING : 音声合成処理が実行されていません<br>
		 * @see ErrCode
		 * @see Status#PLAYING
		 * @see MicroAITalkJni#start(String, int)
		 */
		public ErrCode stop() {
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNIBuffStop());

//			if (errCode != ErrCode.NONE) {
//				/* TODO */
//				Log.w("microAITalkSDK","microAITalkAndroid  Stop Error=" + errCode);
//			}
			return errCode;
		}

		/* kana */
		// mAITalkErrCode mAITalk_Kana_Start(const char* inputText,void (*kanaCallback)(const char*,const char*));
		/**
		 * テキストからカナ変換メソッド
		 * @param inputText
		 * @return
		 * @throws UnsupportedEncodingException
		 */
		public ErrCode kanaStart(String inputText) throws UnsupportedEncodingException {
			ErrCode errCode;

			errCode = ErrCode.convertFromNativeErrCode(JNIKanaStart(inputText.getBytes("SJIS")));

			return errCode;
		}
		
		// mAITalkErrCode mAITalk_Kana_Stop( );
		/**
		 * テキストからカナ変換メソッドを途中で停止させるメソッド
		 * @return
		 */
		public ErrCode kanaStop() {
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNIKanaStop());
			return errCode;
		}

		/* ----- OPTION ----------------------------------------------- */
		/** micro AITalk APIには無い機能 */
		/**
		 * @param inputText
		 * @return
		 * @throws UnsupportedEncodingException
		 */
		public ErrCode buffProcStart(String inputText) throws UnsupportedEncodingException {
			ErrCode errCode;
			errCode = ErrCode.convertFromNativeErrCode(JNIBuffProcStart(inputText.getBytes("SJIS")));
			return errCode;	
		}
		
		/**
		 * 合成処理済みテキストサイズを取得するためのメソッド。<br>
		 * @return 合成済みテキストサイズ(バイト単位)<br>
		 *
		 */
		public long getReadBytes() {
			long ret_val;
			ret_val = JNIGetReadByte();
			return ret_val;
		}

		/**
		 * ネイティブオーディオの音声エフェクトをON/OFFするメソッド。(micro AITalkとは直接関係ない)
		 *
		 */
		public void nativeAudioEffectSwitch(boolean onoff ) {		
			JNINativeAudioEffect(onoff);
		}
		/*------- native I/F -------------------------------------------*/
		/* normal */
		/**
		 * JNIメソッド(バージョン情報取得)
		 * @return micro AITalk SDKバージョン情報
		 */
		private native String JNIGetVersionInfo();

		/**
		 * JNIメソッド(ステイト情報取得)
		 * @return micro AITalk SDK ステイト情報
		 */
		private native int JNIGetStateInfo();

		/**
		 * JNIメソッド(初期化)
		 * @return エラー情報
		 */
		private native int JNIInit();

		/**
		 * JNIメソッド(言語処理辞書ロード)
		 * @return エラー情報
		 */
		private native int JNILangLoad(boolean LangType);
		/**
		 * JNIメソッド(ユーザ単語辞書ロード)
		 * @return エラー情報
		 */
		private native int JNIUdicLoad();
		/**
		 * JNIメソッド(音声辞書ロード)
		 * @param VoiceName 話者名(話者ディレクトリ名)
		 * @return エラー情報
		 */
		private native int JNIVoiceLoad(String VoiceName);

		/**
		 * JNIメソッド(言語処理辞書アンロード)
		 * @return エラー情報
		 */
		private native int JNILangUnload();
		/**
		 * JNIメソッド(ユーザ単語辞書アンロード)
		 * @return エラー情報
		 */
		private native int JNIUdicUnload();
		/**
		 * JNIメソッド(音声辞書アンロード)
		 * @return エラー情報
		 */
		private native int JNIVoiceUnload();


/* TODO パラメタメソッド */
		/**
		 * 音量設定
		 * @param Volume
		 * @return エラー情報
		 */
		private native int JNISetParamVolume(float Volume);

		/**
		 * 話速設定
		 * @param Speed
		 * @return エラー情報
		 */
		private native int JNISetParamSpeed(float Speed);

		/**
		 * ピッチ設定
		 * @param Pitch
		 * @return エラー情報
		 */
		private native int JNISetParamPitch(float Pitch);

		/**
		 * アクセント/抑揚の強さの設定
		 * @param Emph
		 * @return エラー情報
		 */
		private native int JNISetParamEmph(float Emph);

		/**
		 * 入力テキストモードの設定
		 * @param InputMode
		 * @return エラー情報
		 */
		private native int JNISetParamInputMode(int InputMode);


		/**
		 * 中ポーズの設定
		 * @param PauseMiddle
		 * @return エラー情報
		 */
		private native int JNISetParamPauseMiddle(int PauseMiddle);

		/**
		 * 長ポーズの設定
		 * @param PauseLong
		 * @return エラー情報
		 */
		private native int JNISetParamPauseLong(int PauseLong);

		/**
		 * 文末ポーズの設定
		 * @param PauseSentence
		 * @return エラー情報
		 */
		private native int JNISetParamPauseSentence(int PauseSentence);

		/**
		 * 話者設定
		 * @param VoiceName
		 * @return エラー情報
		 */
		private native int JNISetParamVoiceName(String VoiceName);

		/**
		 * JEITA向け 女性話者設定
		 * @param JeitaFemaleName
		 * @return エラー情報
		 */
		private native int JNISetParamJeitaFemaleName(String JeitaFemaleName);

		/**
		 * JEITA向け 男性話者設定
		 * @param JeitaMaleName
		 * @return エラー情報
		 */
		private native int JNISetParamJeitaMaleName(String JeitaMaleName);

		/**
		 * 音量取得
		 * @return 音量またはエラー情報
		 */
		private native float JNIGetParamVolume();

		/**
		 * 話速取得
		 * @return 話速またはエラー情報
		 */
		private native float JNIGetParamSpeed();

		/**
		 * ピッチ取得
		 * @return ピッチまたはエラー情報
		 */
		private native float JNIGetParamPitch();

		/**
		 * アクセント/抑揚の強さの取得
		 * @return アクセント/抑揚の強さまたはエラー情報
		 */
		private native float JNIGetParamEmph();

		/**
		 * 入力テキストモードの取得
		 * @return 入力テキストモードまたはエラー情報
		 */
		private native int JNIGetParamInputMode();


		/**
		 * 中ポーズの取得
		 * @return 中ポーズまたはエラー情報
		 */
		private native int JNIGetParamPauseMiddle();

		/**
		 * 長ポーズの取得
		 * @return 長ポーズまたはエラー情報
		 */
		private native int JNIGetParamPauseLong();

		/**
		 * 文末ポーズの取得
		 * @return 文末またはエラー情報
		 */
		private native int JNIGetParamPauseSentence();

		/**
		 * 話者取得
		 * @return 話者名またはエラー情報
		 */
		private native String JNIGetParamVoiceName();

		/**
		 * JEITA向け 女性話者名取得
		 * @return JEITA向け 女性話者名またはエラー情報
		 */
		private native String JNIGetParamJeitaFemaleName();

		/**
		 * JEITA向け 男性話者名取得
		 * @return JEITA向け 男性話者名またはエラー情報
		 */
		private native String JNIGetParamJeitaMaleName();


		/**
		 * JNIメソッド(終了)
		 * @return エラー情報
		 */
		private native int JNIEnd();

		/* PlayToFile */
		/**
		 * JNIメソッド(音声合成開始(ファイル出力))
		 * @param inputText 入力テキスト
		 * @param fileName 出力ファイル名(.wav拡張子は不要)
		 * @param lipsync  リップシンクON/OFF
		 * @return エラー情報
		 */
		private native int JNIPlayToFile(byte[] inputText, String fileName, boolean lipsync);

		/* buffering */
		/**
		 * JNIメソッド(音声合成開始(バッファ出力))
		 * @param inputText
		 * @param buffSize
		 * @return エラー情報
		 */
		private native int JNIBuffStart(byte[] inputText, int buffSize);

		/**
		 * JNIメソッド(Buffer満タンコールバック設定)
		 * @param cbSet (CB関数をセットする(true)、削除する(false))
		 * @return エラー情報
		 */
		private native int JNIBuffSetFilledCallBack(boolean cbSet);

		/**
		 * JNIメソッド(リップシンクコールバック設定)
		 * @param cbSet (CB関数をセットする(true)、削除する(false))
		 * @return エラー情報
		 */
		private native int JNIBuffSetLipsyncCallBack(boolean cbSet);

		/**
		 * JNIメソッド(センテンスコールバック設定)
		 * @param cbSet (CB関数をセットする(true)、削除する(false))
		 * @return エラー情報
		 */
		private native int JNIBuffSetSentenceCallBack(boolean cbSet);
		
		/**
		 * JNIメソッド(ブックマークコールバック設定)
		 * @param cbSet (CB関数をセットする(true)、削除する(false))
		 * @return エラー情報
		 */
		private native int JNIBuffSetBookMarkCallBack(boolean cbSet);
		
		/**
		 * JNIメソッド(合成データ取得)
		 * @param buff 内部バッファからの音声合成データのコピー先
		 * @return 取得データサイズ。またはエラー/終了情報
		 */
		private native int JNIBuffCheckData(short[] buff);

		/**
		 * JNIメソッド(内部バッファ解放)
		 * @param datalen 解放するデータサイズ
		 * @return エラー情報
		 */
		private native int JNIBuffRelease(int datalen);

		/**
		 * JNIメソッド(合成停止)
		 * @return エラー情報
		 */
		private native int JNIBuffStop();

		/* kana */
		/**
		 * JNIメソッド(Text To Kana開始)
		 * @return エラー情報
		 */
		private native int JNIKanaStart(byte[] inputText);
		/**
		 * JNIメソッド(Text To Kana停止)
		 * @return エラー情報
		 */
		private native int JNIKanaStop( );
		
		/** micro AITalk には無いAPI */
		/* Option */
		/**
		 * JNIメソッド(Text To Speech(Native Audio 出力))
		 * @return エラー情報
		 */
		private native int JNIBuffProcStart(byte[] inputText);
		/**
		 * JNIメソッド(合成済みテキストサイズ取得)
		 * ネイティブ処理にてSentence CallBackを用いて合成済みテキストサイズを取得する。
		 * @return 合成済みテキストサイズ(バイト単位)
		 */
		private native long JNIGetReadByte();
		
		/**
		 * JNIメソッド(ネイティブオーディオのエフェクトスイッチ)(micro AITalkとは直接関係ない)
		 */
		private native void JNINativeAudioEffect(boolean effectOn);
		/*------------------------------------------- native I/F -------*/
}