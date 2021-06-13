package md.meral.recipe

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.navigation.Navigation

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuIflater = menuInflater
        menuIflater.inflate(R.menu.add_food, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.add_food_item) {
            val action = ListFragmentDirections.actionListFragmentToDetailsFragment("fromMenu", 0)
            Navigation.findNavController(this, R.id.fragment).navigate(action)
        }

        return super.onOptionsItemSelected(item)
    }
}