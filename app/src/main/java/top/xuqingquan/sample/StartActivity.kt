package top.xuqingquan.sample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import top.xuqingquan.sample.databinding.ActivityStartBinding
import top.xuqingquan.web.nokernel.WebConfig
import top.xuqingquan.web.publics.AgentWebConfig


class StartActivity : AppCompatActivity() {

    private val binding by lazy { ActivityStartBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        AgentWebConfig.debug()
        binding.btn.setOnClickListener {
            WebConfig.enableTbs()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}