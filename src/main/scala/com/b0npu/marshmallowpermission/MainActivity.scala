package com.b0npu.marshmallowpermission

import android.Manifest
import android.content.{DialogInterface, Intent}
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.PermissionChecker
import android.support.v7.app.{AlertDialog, AppCompatActivity}
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.{Button, Toast}

class MainActivity extends AppCompatActivity with TypedFindView {

  val TAG: String = "M Permission"
  var REQUEST_PERMISSION_CODE: Int = 0x01

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    /* Request Permissionボタンを押した時の動作 */
    val requestButton: Button = findView(TR.requestButton)
    requestButton.setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {

        if (PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
          != PackageManager.PERMISSION_GRANTED) {
          requestCameraPermission
        } else {
          Log.d(TAG, "checkSelfPermission: GRANTED")
          Toast.makeText(
            MainActivity.this,
            "パーミッションは取得済みです！やり直す場合はアプリをアンインストールするか設定から権限を再設定して下さい",
            Toast.LENGTH_LONG
          ).show
          // TODO: Access
        }
      }
    })

    /* AppSettingsボタンを押した時の動作 */
    val settingsButton: Button = findView(TR.settingsButton)
    settingsButton.setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {
        openSettings
      }
    })
  }

  /* アプリ設定を開く */
  private def openSettings: Unit = {
    val openSettingsIntent: Intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val appPackageUri: Uri = Uri.fromParts("package", getPackageName, null)
    openSettingsIntent.setData(appPackageUri)
    startActivity(openSettingsIntent)
  }

  /* パーミッションのリクエスト */
  private def requestCameraPermission: Unit = {

    // 権限チェック
    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)) {

      Log.d(TAG, "shouldShowRequestPermissionRational: 追加説明")
      // 権限が無ければダイアログを出す
      new AlertDialog.Builder(MainActivity.this)
        .setTitle("パーミッションの追加説明")
        .setMessage("このアプリで写真を撮るにはパーミッションが必要です")
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener {
          override def onClick(dialogInterface: DialogInterface, i: Int): Unit = {
            ActivityCompat.requestPermissions(
              MainActivity.this,
              Array[String](Manifest.permission.CAMERA),
              REQUEST_PERMISSION_CODE
            )
          }
        })
        .create
        .show
    } else {

      // 「今後は確認しない」を選択している場合のための権限要求
      ActivityCompat.requestPermissions(
        MainActivity.this,
        Array[String](Manifest.permission.CAMERA),
        REQUEST_PERMISSION_CODE
      )
    }
  }

  /* パーミッションのリクエスト結果を取得 */
  override def onRequestPermissionsResult(
                                           requestCode: Int,
                                           permissions: Array[_root_.java.lang.String],
                                           grantResults: Array[Int]
                                         ): Unit = {

    if (requestCode == REQUEST_PERMISSION_CODE) {
      if (grantResults.length != 1 || grantResults(0) != PackageManager.PERMISSION_GRANTED) {
        Log.d(TAG, "onRequestPermissionResult: DENYED")

        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)) {
          Log.d(TAG, "[show error]")

          new AlertDialog.Builder(MainActivity.this)
            .setTitle("パーミッション取得エラー")
            .setMessage("再取得する場合は再度Requestボタンを押して下さい")
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener {
              override def onClick(dialogInterface: DialogInterface, i: Int): Unit = {
                // ここでrequestCameraPermissionsでも良い
              }
            })
            .create
            .show
        } else {
          Log.d(TAG, "[show app settings guide]")

          new AlertDialog.Builder(MainActivity.this)
            .setTitle("パーミッション取得エラー")
            .setMessage("今後は許可しないが選択されました！！アプリ設定＞権限を確認してください（権限をON/OFFすることで状態はリセットされます）")
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener {
              override def onClick(dialogInterface: DialogInterface, i: Int): Unit = {
                openSettings
              }
            })
            .create
            .show
        }
      } else {
        Log.d(TAG, "onRequestPermissionsResult: GRANTED")
        // 許可されたのでカメラにアクセスする
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }
}
