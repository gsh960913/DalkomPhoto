package com.smcompony.smkotlin

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import com.google.android.gms.common.util.IOUtils.toByteArray
import android.content.pm.PackageManager
import android.content.pm.PackageInfo
import android.os.PersistableBundle
import android.support.v4.app.FragmentActivity
import android.util.Base64
import android.util.Log
import android.view.View
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import com.smcompony.smkotlin.R.id.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class LoginActivity : AppCompatActivity() {

    var auth : FirebaseAuth? = null
    var googleSignInClient : GoogleSignInClient? = null
    var GOOGLE_LOGIN_CODE = 9001
    var callbackManager : CallbackManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        email_login_button.setOnClickListener{
            signinAndSignup()
        }
        google_sign_in_button.setOnClickListener{
            googleLogin()
        }
        facebook_login_button.setOnClickListener {
            facebookLogin()
        }

        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))      // 686277335586-7bsqrptse3vklp5284mp17gjc2qil2pi.apps.googleusercontent.com
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)
            //printHashKey()
            callbackManager = CallbackManager.Factory.create()
    }

    override fun onStart() {
        super.onStart()
        moveMainPage(auth?.currentUser)
    }

    fun printHashKey() {        // 해쉬키 출력
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey = String(Base64.encode(md.digest(), 0))
                Log.i("tag", "printHashKey() Hash Key: $hashKey")
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e("tag", "printHashKey()", e)
        } catch (e: Exception) {
            Log.e("tag", "printHashKey()", e)
        }
    }

    fun googleLogin() {     // 구글 로그인
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    fun facebookLogin(){
        LoginManager.getInstance()
                .logInWithReadPermissions(this, Arrays.asList("public_profile","email"))
        LoginManager.getInstance()
                .registerCallback(callbackManager, object : FacebookCallback<LoginResult>{
                    override fun onSuccess(result: LoginResult?) {
                        // 두번째 방법
                        HandleFaceBookAccessToken(result?.accessToken)
                    }
                    override fun onCancel() {
                        Toast.makeText(this@LoginActivity, "로그인이 취소가 !", Toast.LENGTH_LONG).show()
                    }
                    override fun onError(error: FacebookException?) {
                        Toast.makeText(this@LoginActivity, "로그인이 에러가 !", Toast.LENGTH_LONG).show()
                    }

                })
    }

    fun HandleFaceBookAccessToken(token : AccessToken?){
        var credential = FacebookAuthProvider.getCredential(token?.token!!)
        auth?.signInWithCredential(credential)
                ?.addOnCompleteListener {
                    task ->
                    if (task.isSuccessful) {
                        // 세번째 방법
                        // 유저 계정 생성
                        moveMainPage(task.result?.user)
                    }else {
                        Toast.makeText(this@LoginActivity, task.exception?.message, Toast.LENGTH_LONG).show()
                        // 계정있다면 로그인
                    }
                }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {       // 구글과 Firebase Credential 인증
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, resultCode,data)
        if(requestCode == GOOGLE_LOGIN_CODE){
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            print("결과값입니다 : $result")
            if(result.isSuccess){
                var account = result.signInAccount
                firebaseAuthWithGoogle(account)
                // 두번째 단계계
            }
       }
    }

    fun moveMainPage(user: FirebaseUser?){  // 메인페이지 이동 함수
        if(user != null){
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
        }
    }

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {             // 파이어베이스 와 구글 연동
        var credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credential)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // 유저 계정 생성
                        moveMainPage(task.result?.user)
                    } else if (task.exception?.message.isNullOrEmpty()) {
                        // 에러 메시지 표시
                        Toast.makeText(this@LoginActivity, task.exception?.message, Toast.LENGTH_LONG).show()
                        Toast.makeText(this@LoginActivity, "로그인이 실패했습니다 !", Toast.LENGTH_LONG).show()
                    } else {
                        // 계정있다면 로그인
                    }
            }
    }

    //이메일 회원가입 및 로그인 메소드
    fun signinAndSignup() {
        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
                ?.addOnCompleteListener {
                    task ->
                    progress_bar.visibility = View.GONE
                    if (task.isSuccessful) {
                        //아이디 생성이 성공했을 경우
                        Toast.makeText(this@LoginActivity, task.exception?.message, Toast.LENGTH_LONG).show()
                        //다음페이지 호출
                        moveMainPage(auth?.currentUser)
                    } else if (task.exception?.message.isNullOrEmpty()) {
                        //회원가입 에러가 발생했을 경우
                        Toast.makeText(this@LoginActivity, task.exception?.message, Toast.LENGTH_LONG).show()
                    } else {
                        //아이디 생성도 안되고 에러도 발생되지 않았을 경우 로그인
                        signinEmail()
                    }
                }
    }

    fun signinEmail(){                  // 이메일 로그인
            auth?.signInWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
                    ?.addOnCompleteListener {
                        task ->
                        if(task.isSuccessful){
                            moveMainPage(task.result?.user)
                            // 로그인
                        }else{
                            Toast.makeText(this@LoginActivity,task.exception?.message, Toast.LENGTH_LONG).show()
                            // 에러 메시지 표시
                        }
            }
        }
}
