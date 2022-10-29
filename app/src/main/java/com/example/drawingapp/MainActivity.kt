package com.example.drawingapp

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private var drawingView: drawingView? = null
    private var ibCurrentPaint: ImageButton? = null

    private val OpengalleryLauncher : ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if(result.resultCode == RESULT_OK && result.data!=null){
                val imageBackground : ImageView = findViewById(R.id.iv_background)
                imageBackground.setImageURI(result.data?.data)
            }
        }

    private val GallerybtnLauncher : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        {
            permission ->
            permission.entries.forEach{
                val permissioName = it.key
                val isGranted = it.value
                if (isGranted){
                    if (permissioName == android.Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                        val pickIntent = Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        OpengalleryLauncher.launch(pickIntent)


                    }else{
                        Toast.makeText(this, "per granted for Cam", Toast.LENGTH_SHORT).show()
                    }
                }
                else{
                    if (permissioName == android.Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(this, "Denied Permission", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

     override fun onCreate(savedInstanceState: Bundle?)  {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())
        val llPaintColors : LinearLayout = findViewById(R.id.ll_paint_colors)

        ibCurrentPaint = llPaintColors[1] as ImageButton
        ibCurrentPaint?.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallete_selected)
        )

        val ibbrush : ImageButton = findViewById(R.id.ib_brush)
        ibbrush.setOnClickListener{
            showBrushDialog()
        }

        val galleryBtn: ImageButton = findViewById(R.id.ib_gallery)
         galleryBtn.setOnClickListener {
             requestStoragePermission()
         }

         val ibUndo : ImageButton =findViewById(R.id.ib_undo)
         ibUndo.setOnClickListener{
             drawingView?.onClickUndo()

         }

         val ibsavedo : ImageButton =findViewById(R.id.ib_save)
         ibsavedo.setOnClickListener{
             if (isReadStorageAllowed()){
                 lifecycleScope.launch{
                     val flDrawingView: FrameLayout = findViewById(R.id.fl_container_view)
                     saveBitmapFile(getBitmapFromView(flDrawingView))
                 }
             }

         }

         val ibRedo : ImageButton =findViewById(R.id.ib_redo)
         ibRedo.setOnClickListener{
             drawingView?.onClickRedo()

         }


    }

    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.READ_EXTERNAL_STORAGE
        )

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        ) {
            showRationaleDialog("Permission is Required to access gallery", "You denied")
        } else {
            GallerybtnLauncher.launch(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    private fun showRationaleDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("cancel"){dialog,  _-> dialog.dismiss()
        }
        builder.create().show()
    }

    private fun showBrushDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("select brush Size: ")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.small_btn)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn : ImageButton = brushDialog.findViewById(R.id.medium_btn)
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn : ImageButton = brushDialog.findViewById(R.id.large_btn)
        largeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()

    }

    fun paintClicked(view: View) {
        if (view!==ibCurrentPaint){
            val imageButton = view as ImageButton
            val colortag = imageButton.tag.toString()
            drawingView?.setColor(colortag)
            imageButton?.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallete_selected)
            )

            ibCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallete_normal)
            )

            ibCurrentPaint = view
        }

    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(returnedBitmap)

        val bgDrawable = view.background
        if (bgDrawable != null) {

            bgDrawable.draw(canvas)
        } else {

            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?) : String{
        var result = ""
        withContext(Dispatchers.IO){
            if (mBitmap!=null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)

                    val f = File(externalCacheDir?.absoluteFile.toString()
                            + File.separator + "Drawingapp_" + System.currentTimeMillis()/1000 + ".png")
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result = f.absolutePath

                    runOnUiThread {
                        if (result.isNotEmpty()){
                            Toast.makeText(this@MainActivity, "File saved succesfully: $result", Toast.LENGTH_LONG).show()
                            shareImage(FileProvider.getUriForFile(baseContext,"com.example.drawingapp.fileprovider", f))
                        }
                        else{
                            Toast.makeText(this@MainActivity,"Something Went Wrong",Toast.LENGTH_SHORT).show()
                        }
                    }

                }
                catch (e: Exception){
                result =""
                    e.printStackTrace()

                }
            }
        }
        return result
    }

    private fun shareImage(uri: Uri){




            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/jpeg"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }

    }
