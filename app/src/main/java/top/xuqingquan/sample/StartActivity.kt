package top.xuqingquan.sample

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import top.xuqingquan.web.publics.AgentWebConfig


class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        AgentWebConfig.debug()
        startActivity(Intent(this, MainActivity::class.java))
    }
}