package com.nikhildev.mtranslate

import android.app.ActionBar
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var menuHost : MenuHost
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        auth = FirebaseAuth.getInstance()

        val actionBar: ActionBar? =getActionBar()
        val colorDrawable = ColorDrawable(Color.parseColor("#0F9D58"))
        actionBar?.setBackgroundDrawable(colorDrawable)

        menuHost = this
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu,menu)
                for (i in 0 until menu.size()) {
                    val menuItem = menu.getItem(i)
                    val drawable: Drawable? = menuItem.icon
                    if (drawable != null) {
                        drawable.mutate()
                        drawable.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                    }
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when(menuItem.itemId){
                    R.id.userInfoMenu -> {
                        Toast.makeText(this@MainActivity,"User Info", Toast.LENGTH_SHORT).show()
                        val user = auth.currentUser!!
                        val alertBuilder= AlertDialog.Builder(this@MainActivity)
                        alertBuilder
                            .setTitle("Current User Info")
                            .setMessage("\nId : ${user.uid}\n\nName : ${user.displayName}\nEmail : ${user.email}")
                            .setPositiveButton("OK"){ _,_ -> }
                            .create()
                        alertBuilder.show()
                        true
                    }
                    R.id.logOutMenu ->{
                        Toast.makeText(this@MainActivity,"Signing Out User", Toast.LENGTH_SHORT).show()
                        auth.signOut()
                        googleSignInClient.signOut()
                        startActivity(Intent(this@MainActivity, SignInView::class.java))
                        true
                    }
                    else -> false
                }
            }
        })

        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        val viewPager: ViewPager = findViewById(R.id.viewPager)

        val fragments = listOf<Fragment>(
            TranslateFragment.newInstance(),
            ShowTranslationsFragment.newInstance())
        val titles = listOf("Translator", "Show Data")

        val adapter = PagerAdapter(supportFragmentManager, fragments, titles)
        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)
    }
}

class PagerAdapter(fm: FragmentManager, private val fragments: List<Fragment>, private val titles: List<String>) :
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return fragments[position]
    }

    override fun getCount(): Int {
        return fragments.size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return titles[position]
    }
}