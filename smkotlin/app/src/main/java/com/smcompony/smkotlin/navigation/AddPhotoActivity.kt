package com.smcompony.smkotlin.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.smcompony.smkotlin.R
import com.smcompony.smkotlin.R.id.*
import com.smcompony.smkotlin.model.ContentDTO
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {


    var PICK_IMAGE_FROM_ALBUM = 0
    var storage : FirebaseStorage? = null
    var photoUri: Uri? = null
    var auth : FirebaseAuth? = null
    var firestore : FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        // 저장소 초기화
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // 앨범 열기
        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent,PICK_IMAGE_FROM_ALBUM)

        // 앨범 업로드 이벤트
        addphoto_btn_upload.setOnClickListener{
            contentUpload()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_FROM_ALBUM) {
            if(resultCode == Activity.RESULT_OK){
                // 선택된 이미지 경로
                photoUri = data?.data
                addphoto_image.setImageURI(photoUri)
            }else {
                finish()
                // 선택없이 앨범을 떠날경우 액티비티 인텐트
            }
        }
    }

    fun contentUpload() {
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + "_.png"
        var storageRef = storage?.reference?.child("images")?.child(imageFileName)

        //
//        storageRef?.putFile(photoUri!!)?.continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
//            return@continueWithTask storageRef.downloadUrl
//        }?.addOnSuccessListener { uri ->
//            var contentDTO = ContentDTO()
//
//            // 이미지의 다운로드 url
//            contentDTO.imageUrl = uri.toString()
//
//            // 유저의 uid 삽입
//            contentDTO.uid = auth?.currentUser?.uid
//
//            // 유저아이디 삽입
//            contentDTO.userId = auth?.currentUser?.email
//
//            // 내용 삽입
//            contentDTO.explain = addphoto_edit_explain.text.toString()
//
//            // 타임스탬프 삽입
//            contentDTO.timestamp = System.currentTimeMillis()
//
//            firestore?.collection("images")?.document()?.set(contentDTO)
//
//            setResult(Activity.RESULT_OK)
//
//            finish()
//            }
//        }
        // 파일 업로드 (콜 백 메소드)
        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                var contentDTO = ContentDTO()

                // 이미지의 다운로드 url
                contentDTO.imageUrl = uri.toString()

                // 유저의 uid 삽입
                contentDTO.uid = auth?.currentUser?.uid

                // 유저 이메일 삽입
                contentDTO.userId = auth?.currentUser?.email

                // 내용 삽입
                contentDTO.explain = addphoto_edit_explain.text.toString()

                // 타임스탬프 삽입
                contentDTO.timestamp = System.currentTimeMillis()

                firestore?.collection("images")?.document()?.set(contentDTO)

                setResult(Activity.RESULT_OK)

                finish()
            }

        }
    }
}
