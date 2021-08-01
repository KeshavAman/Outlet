package com.example.outlet

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.outlet.databinding.ActivityVerificationBinding
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import kotlinx.android.synthetic.main.activity_verification.*
import java.util.concurrent.TimeUnit

class VerificationActivity : AppCompatActivity() {

    private lateinit var binding : ActivityVerificationBinding
    var auth : FirebaseAuth? = null
    private val TAG = "VerificationActivity Tag"
    private var forceResendingToken : PhoneAuthProvider.ForceResendingToken?=null
    private var mCallbacks : PhoneAuthProvider.OnVerificationStateChangedCallbacks?=null
    private var mVerificationId : String?=null
    private lateinit var progressDialog: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.phoneLl.visibility = View.VISIBLE
        binding.codeLl.visibility = View.GONE

        auth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please Wait")
        progressDialog.setCanceledOnTouchOutside(false)

        if (auth!!.currentUser!= null){
            val intent = Intent(this@VerificationActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        supportActionBar?.hide()
        val phoneNumber = intent.getStringExtra("phoneNumber")
        binding.phoneLabel.text = "Verify $phoneNumber"
        mCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:$phoneAuthCredential")
                signInWithPhoneAuthCredential(phoneAuthCredential)
            }
            override fun onVerificationFailed(e: FirebaseException) {
                progressDialog.dismiss()
                Log.w(TAG, "onVerificationFailed", e)

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                }

                // Show a message and update the UI
            }
            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:$verificationId")

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId
                forceResendingToken = token
                progressDialog.dismiss()
                binding.phoneLl.visibility = View.GONE
                binding.codeLl.visibility = View.VISIBLE
                Toast.makeText(this@VerificationActivity, "Verification Code Sent",Toast.LENGTH_SHORT).show()

            }
        }
        binding.continueBtn.setOnClickListener {
            val phone = binding.editNumber.text.toString().trim()
            if(TextUtils.isEmpty(phone)){
                Toast.makeText(this@VerificationActivity,"Please enter phone number",Toast.LENGTH_SHORT).show()
            }
            else{
                sendVerificationCode(phone)
            }
        }

        binding.continueBtn01.setOnClickListener {

            val code = binding.otpView.text.toString().trim()
            if(TextUtils.isEmpty(code)){
                Toast.makeText(this@VerificationActivity,"Please enter OTP",Toast.LENGTH_SHORT).show()
            }
            else{
                verifyPhoneNumberWithCode(mVerificationId , code)
            }

        }

    }
    private fun sendVerificationCode(phone: String) {
        Log.d(TAG,"SendVerificationCode:$phone")
        progressDialog.setMessage("Verifying Phone Number")
        progressDialog.show()
        val options = PhoneAuthOptions.newBuilder(auth!!)
            .setPhoneNumber(phone)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity (for callback binding)
            .setCallbacks(mCallbacks!!) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)

    }
    private fun verifyPhoneNumberWithCode(verificationId : String? , code: String){
        progressDialog.setMessage("Verifying Code")
        progressDialog.show()
        Log.d(TAG,"verifyPhoneNumberWithCode:$verificationId , $code")
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        progressDialog.setMessage("Logging In")

        auth!!.signInWithCredential(credential)
            .addOnSuccessListener { e->
                progressDialog.dismiss()
                val phone = auth!!.currentUser?.phoneNumber
                Toast.makeText(this,"Logged in as $phone",Toast.LENGTH_SHORT).show()
                startActivity(Intent(this,SetupProfileActivity::class.java))
            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(this,"$(e.message)",Toast.LENGTH_SHORT).show()
            }
    }
}

