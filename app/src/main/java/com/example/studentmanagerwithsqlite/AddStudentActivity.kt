package com.example.studentmanagerwithsqlite

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class AddStudentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_student)

        val inputStudentName = intent.getStringExtra("studentName").toString().trim()
        val inputStudentId = intent.getStringExtra("studentId").toString().trim()
        val position = intent.getIntExtra("position", -1)
        val recId = intent.getIntExtra("recId", -1)

        if(inputStudentName.isNotEmpty() && inputStudentId.isNotEmpty() && position > -1){
            findViewById<EditText>(R.id.edit_hoten).setText(inputStudentName)
            findViewById<EditText>(R.id.edit_mssv).setText(inputStudentId)
            findViewById<TextView>(R.id.textView).setText("CHỈNH SỬA THÔNG TIN SINH VIÊN")
        }

        findViewById<Button>(R.id.button_submit_add_student).setOnClickListener {
            AlertDialog.Builder(this)
                .setIcon(R.drawable.baseline_question_mark_24)
                .setTitle("Xác nhận lưu thông tin")
                .setMessage("Bạn chắc chắn muốn lưu thông tin?")
                .setPositiveButton("Ok") { _, _ ->
                    val newName = findViewById<EditText>(R.id.edit_hoten).text.toString().trim()
                    val newId = findViewById<EditText>(R.id.edit_mssv).text.toString().trim()
                    if(newName.isEmpty() || newId.isEmpty()){
                        Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_LONG).show()
                    } else {
                        intent.putExtra("newName", newName)
                        intent.putExtra("newId", newId)
                        if(position > -1) intent.putExtra("position", position)
                        if(recId > -1) intent.putExtra("recID", recId)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }
                }
                .setNegativeButton("Cancle", null)
                .setCancelable(false)
                .show()
        }

        findViewById<Button>(R.id.button_cancle).setOnClickListener{
            finish()
        }
    }
}