package com.b0npu.mpermissions

import android.Manifest
import android.content.pm.PackageManager
import android.content.{ContentResolver, ContentUris, DialogInterface, Intent}
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.{BaseColumns, MediaStore, Settings}
import android.support.v4.app.ActivityCompat
import android.support.v4.content.PermissionChecker
import android.support.v7.app.{AlertDialog, AppCompatActivity}
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.{Button, ImageView, Toast}

class MainActivity extends AppCompatActivity with TypedFindView {

  /**
    * フィールドの定義
    *
    * requestPermissionsメソッドで権限を要求した際に
    * コールバックメソッドのonRequestPermissionsResultメソッドに渡す定数を定義
    * Logを所々で表示するのでTagの変数も定義
    * (自クラスで使うだけのフィールドはprivateにして明示的に非公開にしてます)
    */
  private val REQUEST_CAMERA_PERMISSION_CODE: Int = 0x01
  private val REQUEST_READ_STORAGE_PERMISSION_CODE: Int = 0x02

  private val TAG: String = "M Permission"

  /**
    * アプリの画面を生成
    *
    * アプリを起動するとonCreateが呼ばれてActivityが初期化され
    * setContentViewでレイアウトがビューに表示される
    */
  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    /**
      * Request Permissionボタンを押す
      *
      * CAMERAのパーミッションの状態を確認して
      * 権限が許可されていない場合はrequestCameraPermissionメソッドを呼んで
      * 権限の許可を要求する
      * 権限が許可されていればToastで通知だけする(CAMERAは起動しません)
      */
    val requestButton: Button = findView(TR.requestButton)
    requestButton.setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {

        if (PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
          != PackageManager.PERMISSION_GRANTED) {
          requestCameraPermission
        } else {
          Log.d(TAG, "checkSelfPermission: CAMERA GRANTED")
          Toast.makeText(
            MainActivity.this,
            "パーミッションは取得済みです！やり直す場合はアプリをアンインストールするか設定から権限を再設定して下さい",
            Toast.LENGTH_LONG
          ).show
          /* TODO: 必要ならここでカメラを起動する */
        }
      }
    })

    /**
      * AppSettingsボタンを押す
      *
      * パーミッションの状態の確認や手動設定を行うために
      * openSettingsメソッドを呼んでアプリの設定画面を開く
      */
    val settingsButton: Button = findView(TR.settingsButton)
    settingsButton.setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {
        openSettings
      }
    })

    /**
      * View Imageボタンを押す
      *
      * READ_EXTERNAL_STORAGEのパーミッションの状態を確認して
      * 権限が許可されていない場合はrequestReadStoragePermissionソッドを呼んで
      * 権限の許可を要求する
      * 権限が許可されていればToastで通知しつつviewBackgroundImageメソッドを呼んで
      * SDカードに保存されている画像の最初の1枚を背景に表示する
      */
    val viewImageButton: Button = findView(TR.viewImageButton)
    viewImageButton.setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {

        if (PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
          != PackageManager.PERMISSION_GRANTED) {
          requestReadStoragePermission
        } else {
          Log.d(TAG, "checkSelfPermission: READ STORAGE GRANTED")
          Toast.makeText(
            MainActivity.this,
            "パーミッションは取得済みです！",
            Toast.LENGTH_LONG
          ).show
          /* 画像を背景に表示する */
          viewBackgroundImage
        }
      }
    })
  }

  /**
    * openSettingsメソッドの定義
    *
    * インテントを使ってアプリの設定画面を開く
    */
  private def openSettings: Unit = {

    val appSettingsIntent: Intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val appPackageUri: Uri = Uri.fromParts("package", getPackageName, null)

    /* インテントにアプリのURIを指定してアプリ情報の画面を開く */
    appSettingsIntent.setData(appPackageUri)
    startActivity(appSettingsIntent)
  }

  /**
    * viewBackgroundImageメソッドの定義
    *
    * SDカードに保存されている画像の最初の1枚を背景に表示する
    * TODO: SDカードに画像が無い場合のエラー処理をしてないので注意
    */
  private def viewBackgroundImage: Unit = {

    /* SDカードの画像データのURIに問い合わせをして検索結果をCursorに格納する */
    val imageMediaStoreUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val mediaContentResolver: ContentResolver = getContentResolver
    val pictureCursor: Cursor = mediaContentResolver.query(imageMediaStoreUri, null, null, null, null)

    Log.v("Media", "Cursor Column Names" + pictureCursor.getColumnNames.toString)
    Log.v("Media", "Image File:" + pictureCursor.getCount)

    /* Cursorに格納した画像データの検索結果から1枚目の画像のIDを取得する */
    pictureCursor.moveToFirst
    val pictureId: Long = pictureCursor.getLong(pictureCursor.getColumnIndex(BaseColumns._ID))

    /* 画像データのURIとIDから画像(ビットマップ画像)を取得する */
    val bmpImageUri: Uri = ContentUris.withAppendedId(imageMediaStoreUri, pictureId)
    val bmpImage: Bitmap = MediaStore.Images.Media.getBitmap(mediaContentResolver, bmpImageUri)

    /* ImageViewに画像(ビットマップ画像)を表示する */
    val backgroundImageView: ImageView = findView(TR.backgroundImage)
    backgroundImageView.setImageBitmap(bmpImage)
  }

  /**
    * requestCameraPermissionメソッドの定義
    *
    * CAMERAのパーミッションの許可(権限取得)を要求する
    * shouldShowRequestPermissionRationaleメソッドを使って
    * 以前にパーミッションの許可を拒否されたことがあるか確認し
    * 拒否されたことがある場合はパーミッションの許可が必要な理由を
    * ダイアログに表示してからパーミッションの許可を要求する
    */
  private def requestCameraPermission: Unit = {

    /* パーミッションの許可を拒否されたことがあるか確認する */
    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)) {

      Log.d(TAG, "shouldShowRequestPermissionRational: CAMERAの権限取得に関する追加説明")

      /* パーミッションの許可を拒否されたことがあれば許可が必要な理由を説明してから許可を要求する */
      new AlertDialog.Builder(MainActivity.this)
        .setTitle("パーミッションの追加説明")
        .setMessage("このアプリで写真を撮るにはパーミッションが必要です")
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener {

          override def onClick(dialogInterface: DialogInterface, i: Int): Unit = {
            /* パーミッションの許可を要求 */
            ActivityCompat.requestPermissions(
              MainActivity.this,
              Array[String](Manifest.permission.CAMERA),
              REQUEST_CAMERA_PERMISSION_CODE
            )
          }
        })
        .create
        .show

    } else {
      /* 初回要求時か「今後は確認しない」を選択されている場合のパーミッションの許可の要求 */
      ActivityCompat.requestPermissions(
        MainActivity.this,
        Array[String](Manifest.permission.CAMERA),
        REQUEST_CAMERA_PERMISSION_CODE
      )
    }
  }

  /**
    * requestReadStoragePermissionメソッドの定義
    *
    * READ_EXTERNAL_STORAGEのパーミッションの許可(権限取得)を要求する
    * shouldShowRequestPermissionRationaleメソッドを使って
    * 以前にパーミッションの許可を拒否されたことがあるか確認し
    * 拒否されたことがある場合はパーミッションの許可が必要な理由を
    * ダイアログに表示してからパーミッションの許可を要求する
    */
  private def requestReadStoragePermission: Unit = {

    /* パーミッションの許可を拒否されたことがあるか確認する */
    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

      Log.d(TAG, "shouldShowRequestPermissionRational: ReadStorageの権限取得に関する追加説明")

      /* パーミッションの許可を拒否されたことがあれば許可が必要な理由を説明してから許可を要求する */
      new AlertDialog.Builder(MainActivity.this)
        .setTitle("パーミッションの追加説明")
        .setMessage("このアプリで画像を表示するにはパーミッションが必要です")
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener {

          override def onClick(dialogInterface: DialogInterface, i: Int): Unit = {
            /* パーミッションの許可を要求 */
            ActivityCompat.requestPermissions(
              MainActivity.this,
              Array[String](Manifest.permission.READ_EXTERNAL_STORAGE),
              REQUEST_READ_STORAGE_PERMISSION_CODE
            )
          }
        })
        .create
        .show

    } else {
      /* 初回要求時か「今後は確認しない」を選択されている場合のパーミッションの許可の要求 */
      ActivityCompat.requestPermissions(
        MainActivity.this,
        Array[String](Manifest.permission.READ_EXTERNAL_STORAGE),
        REQUEST_READ_STORAGE_PERMISSION_CODE
      )
    }
  }

  /**
    * onRequestPermissionsResultメソッドをオーバーライド
    *
    * このメソッドはrequestPermissionsメソッドのコールバックメソッドで
    * requestPermissionsメソッドでパーミッションの許可を要求した結果を取得する
    * 引数のrequestCodeで要求されたパーミッションを区別し
    * grantResultの要素でパーミッションの許可・不許可を確認する
    */
  override def onRequestPermissionsResult(requestCode: Int, permissions: Array[_root_.java.lang.String], grantResults: Array[Int]): Unit = {

    /* 要求されたパーミッションによって対応が変わるので何のパーミッションか確認する */
    requestCode match {

      case REQUEST_CAMERA_PERMISSION_CODE ⇒
        /* パーミッションの要求が拒否されていた場合はダイアログに表示する */
        if (grantResults.length != 1 || grantResults(0) != PackageManager.PERMISSION_GRANTED) {
          Log.d(TAG, "onRequestPermissionResult: DENIED")

          /* 「今後は確認しない」が選択されていなければ許可が必要な理由を説明する */
          if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)) {
            Log.d(TAG, "[show error]")

            new AlertDialog.Builder(MainActivity.this)
              .setTitle("パーミッション取得エラー")
              .setMessage("再取得する場合は再度Requestボタンを押して下さい")
              .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener {

                override def onClick(dialogInterface: DialogInterface, i: Int): Unit = {
                  /* TODO: ここでrequestCameraPermissionsでも良い */
                }
              })
              .create
              .show

          } else {
            /* 「今後は確認しない」を選択されている場合はアプリの設定画面を開く */
            Log.d(TAG, "[show app settings guide]")

            new AlertDialog.Builder(MainActivity.this)
              .setTitle("パーミッション取得エラー")
              .setMessage("今後は許可しないが選択されました！！アプリ設定＞権限を確認してください（権限をON/OFFすることで状態はリセットされます）")
              .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener {

                override def onClick(dialogInterface: DialogInterface, i: Int): Unit = {
                  /* アプリの設定画面を開いて手動で許可してもらう */
                  openSettings
                }
              })
              .create
              .show
          }

        } else {
          /* パーミッションが許可された場合はToastで通知する(CAMERAは起動しません) */
          Log.d(TAG, "onRequestPermissionsResult: CAMERA GRANTED")
          /* TODO: 必要ならカメラを起動する */
          Toast.makeText(
            MainActivity.this,
            "パーミッションを取得しました！",
            Toast.LENGTH_LONG
          ).show
        }

      case REQUEST_READ_STORAGE_PERMISSION_CODE ⇒
        /* パーミッションの要求が拒否されていた場合はダイアログに表示する */
        if (grantResults.length != 1 || grantResults(0) != PackageManager.PERMISSION_GRANTED) {
          Log.d(TAG, "onRequestPermissionResult: DENIED")

          /* 「今後は確認しない」が選択されていなければ許可が必要な理由を説明する */
          if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Log.d(TAG, "[show error]")

            new AlertDialog.Builder(MainActivity.this)
              .setTitle("パーミッション取得エラー")
              .setMessage("再取得する場合は再度Requestボタンを押して下さい")
              .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener {

                override def onClick(dialogInterface: DialogInterface, i: Int): Unit = {
                  /* TODO: ここでrequestCameraPermissionsでも良い */
                }
              })
              .create
              .show

          } else {
            /* 「今後は確認しない」を選択されている場合はアプリの設定画面を開く */
            Log.d(TAG, "[show app settings guide]")

            new AlertDialog.Builder(MainActivity.this)
              .setTitle("パーミッション取得エラー")
              .setMessage("今後は許可しないが選択されました！！アプリ設定＞権限を確認してください（権限をON/OFFすることで状態はリセットされます）")
              .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener {

                override def onClick(dialogInterface: DialogInterface, i: Int): Unit = {
                  /* アプリの設定画面を開いて手動で許可してもらう */
                  openSettings
                }
              })
              .create
              .show
          }

        } else {
          /* パーミッションが許可された場合は画像を表示する */
          Log.d(TAG, "onRequestPermissionsResult: GRANTED")
          viewBackgroundImage
        }
    }
  }
}
