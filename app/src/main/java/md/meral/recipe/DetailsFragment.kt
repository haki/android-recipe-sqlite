package md.meral.recipe

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import java.io.ByteArrayOutputStream
import java.lang.Exception

class DetailsFragment : Fragment() {

    var image: Uri? = null
    var imageBitmap: Bitmap? = null
    private lateinit var selectImage: ImageView

    private lateinit var foodName: String
    private lateinit var foodIngredients: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val saveButton: Button = view.findViewById(R.id.saveButton)
        saveButton.setOnClickListener {
            foodName = view.findViewById<EditText>(R.id.food_name).text.toString()
            foodIngredients = view.findViewById<EditText>(R.id.food_ingredients).text.toString()
            save(it)
        }

        selectImage = view.findViewById(R.id.add_image)
        selectImage.setOnClickListener {
            selectImage(it)
        }

        arguments?.let {
            val receivedInfo = DetailsFragmentArgs.fromBundle(it).info

            val foodNameText: EditText = view.findViewById(R.id.food_name)
            val foodIngredientsText: EditText = view.findViewById(R.id.food_ingredients)
            if (receivedInfo.equals("fromMenu")) {
                //came to add a new food
                foodNameText.setText("")
                foodIngredientsText.setText("")
                saveButton.visibility = View.VISIBLE
            } else {
                //came to see the food created before
                saveButton.visibility = View.INVISIBLE

                val selectedId = DetailsFragmentArgs.fromBundle(it).id

                context?.let {
                    try {
                        val db = it.openOrCreateDatabase("Foods", Context.MODE_PRIVATE, null)
                        val cursor = db.rawQuery(
                            "SELECT * FROM foods WHERE id = ?",
                            arrayOf(selectedId.toString())
                        )

                        val foodNameIndex = cursor.getColumnIndex("food_name")
                        val foodIngredientsIndex = cursor.getColumnIndex("food_ingredients")
                        val foodImage = cursor.getColumnIndex("image")

                        while (cursor.moveToNext()) {
                            foodNameText.setText(cursor.getString(foodNameIndex))
                            foodIngredientsText.setText(cursor.getString(foodIngredientsIndex))

                            val byteArray = cursor.getBlob(foodImage)
                            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                            selectImage.setImageBitmap(bitmap)
                        }

                        cursor.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun save(view: View) {
        //Saving to SQLite
        if (imageBitmap != null) {
            val smallBitmap = reduceBitmap(imageBitmap!!, 300)
            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
            val byteArray = outputStream.toByteArray()

            try {
                context?.let {
                    val database = it.openOrCreateDatabase("Foods", Context.MODE_PRIVATE, null)
                    database.execSQL("CREATE TABLE IF NOT EXISTS foods (id INTEGER PRIMARY KEY, food_name VARCHAR, food_ingredients VARCHAR, image BLOB)")
                    val sqlString =
                        "INSERT INTO foods (food_name, food_ingredients, image) VALUES (?, ?, ?)"
                    val statement = database.compileStatement(sqlString)
                    statement.bindString(1, foodName)
                    statement.bindString(2, foodIngredients)
                    statement.bindBlob(3, byteArray)
                    statement.execute()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val action = DetailsFragmentDirections.actionDetailsFragmentToListFragment()
            Navigation.findNavController(view).navigate(action)
        }
    }

    fun selectImage(view: View) {
        activity?.let {
            if (ContextCompat.checkSelfPermission(
                    it.applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //not allowed, we need to ask permission
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            } else {
                //permission already granted, go to gallery without asking again
                val galeryIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galeryIntent, 2)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //we got permission
                val galeryIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galeryIntent, 2)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK && data != null) {
            image = data.data

            try {
                context?.let {
                    if (image != null) {
                        if (Build.VERSION.SDK_INT >= 28) {
                            val source = ImageDecoder.createSource(it.contentResolver, image!!)
                            imageBitmap = ImageDecoder.decodeBitmap(source)
                            selectImage.setImageBitmap(imageBitmap)
                        } else {
                            imageBitmap =
                                MediaStore.Images.Media.getBitmap(it.contentResolver, image)
                            selectImage.setImageBitmap(imageBitmap)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    fun reduceBitmap(userBitmap: Bitmap, maxSize: Int): Bitmap {
        var width = userBitmap.width
        var height = userBitmap.height

        val bitmapRate: Double = width.toDouble() / height.toDouble()

        if (bitmapRate > 1) {
            // image is horizontal
            width = maxSize
            height = (width / bitmapRate).toInt()
        } else {
            // image is vertical
            height = maxSize
            width = (height * bitmapRate).toInt()
        }

        return Bitmap.createScaledBitmap(userBitmap, width, height, true)
    }
}