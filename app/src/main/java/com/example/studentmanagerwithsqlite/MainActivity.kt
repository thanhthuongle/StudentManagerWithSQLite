package com.example.studentmanagerwithsqlite

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    private lateinit var db: SQLiteDatabase
    val tableName = "student_data"
    private val students = mutableListOf<StudentModel>()
    private val studentAdapter = StudentAdapter(students)
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private lateinit var launcher1: ActivityResultLauncher<Intent>
    @SuppressLint("Recycle")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }

        db = SQLiteDatabase.openDatabase(filesDir.path + "/my_data", null, SQLiteDatabase.CREATE_IF_NECESSARY)

        initialDataSeed()

        getData()

        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),{
            if(it.resultCode == Activity.RESULT_OK) {
                val newName = it.data?.getStringExtra("newName").toString()
                val newId = it.data?.getStringExtra("newId").toString()
                addNewStudent(newName, newId)
                getData()
                studentAdapter.notifyDataSetChanged()

                Toast.makeText(this, "Thêm sinh viên mới thành công", Toast.LENGTH_LONG).show()
            }
        })

        launcher1 = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),{
            if(it.resultCode == Activity.RESULT_OK) {
                val recId = it.data?.getIntExtra("recId", -1)
                val newName = it.data?.getStringExtra("newName").toString()
                val newId = it.data?.getStringExtra("newId").toString()
                val position = it.data?.getIntExtra("position", -1)
                if(position!! > -1 && recId!! > -1){
//                    students[position].studentName = newName
//                    students[position].studentId = newId
                    updateStudent(recId, newName, newId)
                    getData()
                    studentAdapter.notifyDataSetChanged()
                    Toast.makeText(this, "Thay đổi thông tin sinh viên thành công", Toast.LENGTH_LONG).show()
                } else{
                    Toast.makeText(this, "Thay đổi thông tin sinh viên thất bại", Toast.LENGTH_LONG).show()
                }

            }
        })

        val studentList = findViewById<ListView>(R.id.student_list)
        studentList.adapter = studentAdapter

        registerForContextMenu(studentList)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.button_add_student -> {
                val intent = Intent(this, AddStudentActivity::class.java)
                launcher.launch(intent)
//                startActivity(intent)
//                Toast.makeText(this, "Share action", Toast.LENGTH_LONG).show()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        menuInflater.inflate(R.menu.context_menu, menu)
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    @SuppressLint("ShowToast")
    override fun onContextItemSelected(item: MenuItem): Boolean {
        val pos = (item.menuInfo as AdapterContextMenuInfo).position
        when(item.itemId) {
            R.id.button_edit_student -> {
                val recId = students[pos].recID
                val studentName = students[pos].studentName
                val studentId = students[pos].studentId
                val intent = Intent(this, AddStudentActivity::class.java)
                intent.putExtra("recId", recId)
                intent.putExtra("studentName", studentName)
                intent.putExtra("studentId", studentId)
                intent.putExtra("position", pos)
                launcher1.launch(intent)
            }

            R.id.button_delete_student -> {
                AlertDialog.Builder(this)
                    .setIcon(R.drawable.baseline_question_mark_24)
                    .setTitle("Xác nhận xóa sinh viên!")
                    .setMessage("Bạn chắc chắn muốn xóa sinh viên:\n${students[pos].studentName}-${students[pos].studentId}")
                    .setPositiveButton("Ok") { _, _ ->
                        val recId = students[pos].recID
                        val studentName = students[pos].studentName
                        val studentId = students[pos].studentId
                        students.removeAt(pos)
                        studentAdapter.notifyDataSetChanged()
                        Snackbar.make(findViewById(R.id.main), "Đã xóa 1 học sinh",  Snackbar.LENGTH_LONG)
                            .setAction("Hoàn tác") {
                                students.add(pos, StudentModel(recId, studentName, studentId))
                                studentAdapter.notifyDataSetChanged()
                            }
                            .addCallback(object : Snackbar.Callback() {
                                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                    super.onDismissed(transientBottomBar, event)
                                    // Khi Snackbar bị đóng, dù là vì người dùng nhấn vào "Hoàn tác" hay tự động đóng
                                    if (event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT || event == Snackbar.Callback.DISMISS_EVENT_CONSECUTIVE) {
                                        // Thực hiện hành động khi Snackbar đóng tự động (do hết thời gian)
                                        // Hoặc khi người dùng kéo hoặc nhấn ngoài để đóng Snackbar
                                        // Log.d("Snackbar", "Snackbar đã đóng")
                                        deleteStudent(recId)
                                        Log.d("Snackbar", "ĐÃ xóa sv khỏi db")
                                    }
                                }
                            })
                            .show()
                    }
                    .setNegativeButton("Cancle", null)
                    .setCancelable(false)
                    .show()
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun initialDataSeed() {
        db.beginTransaction()
        try {
            db.execSQL("create table '$tableName'(" +
                    "recID integer primary key autoincrement," +
                    "studentName text," +
                    "studentId text)")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Nguyễn Văn An', 'SV001')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Trần Thị Bảo', 'SV002')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Lê Hoàng Cường', 'SV003')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Phạm Thị Dung', 'SV004')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Đỗ Minh Đức', 'SV005')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Vũ Thị Hoa', 'SV006')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Hoàng Văn Hải', 'SV007')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Bùi Thị Hạnh', 'SV008')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Đinh Văn Hùng', 'SV009')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Nguyễn Thị Linh', 'SV0010')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Phạm Văn Long', 'SV011')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Trần Thị Mai', 'SV012')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Lê Thị Ngọc', 'SV013')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Vũ Văn Nam', 'SV014')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Hoàng Thị Phương', 'SV015')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Đỗ Văn Quân', 'SV016')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Nguyễn Thị Thu', 'SV017')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Trần Văn Tài', 'SV018')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Phạm Thị Tuyết', 'SV019')")
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('Lê Văn Vũ', 'SV020')")
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    private fun getData() {
        students.clear()
        db.beginTransaction()
        try {
            val cs = db.rawQuery("select * from '$tableName' order by recID DESC", null)
            if(cs.moveToFirst()){
                do {
                    val recID = cs.getInt(0)
                    val studentName = cs.getString(1)
                    val studentId = cs.getString(2)
                    students.add(StudentModel(recID, studentName, studentId))
                } while (cs.moveToNext())
                cs.close()
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            studentAdapter.notifyDataSetChanged()
            db.endTransaction()
        }
    }

    private fun addNewStudent(newName: String, newId: String) {
        db.beginTransaction()
        try {
            db.execSQL("insert into '${tableName}'(studentName, studentId) values('$newName','$newId')")
            db.setTransactionSuccessful()
        } catch (e: Exception){
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    private fun updateStudent(recID: Int, newName: String, newId: String) {
        db.beginTransaction()
        try {
            db.execSQL("update '${tableName}' set studentName='$newName', studentId='$newId' where recID='$recID'")
            db.setTransactionSuccessful()
        } catch (e: Exception){
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteStudent(recID: Int) {
        db.beginTransaction()
        try {
            db.execSQL("delete from '$tableName' where recID = '$recID'")
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    override fun onStop() {
//        if(::db.isInitialized) db.close()
        super.onStop()
    }

    override fun onDestroy() {
        if(::db.isInitialized) db.close()
        super.onDestroy()
    }
}