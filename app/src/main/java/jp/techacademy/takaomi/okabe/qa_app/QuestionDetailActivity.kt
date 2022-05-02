package jp.techacademy.takaomi.okabe.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_question_send.progressBar

class QuestionDetailActivity : AppCompatActivity() , DatabaseReference.CompletionListener{

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private lateinit var mFavoritesRef: DatabaseReference
    private lateinit var mFavoritesUserRef: DatabaseReference

    private var favoriteflg = 0

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            val answer = Answer(body, name, uid, answerUid)

            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()

        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    private val mFavoriteEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {

            val qUid = dataSnapshot.key ?: ""
            if (qUid == mQuestion.questionUid) {    //渡ってきたquestionUidがデータベースにあるとき
                favoriteflg = 1                     //1つでも合えば1にする
            }
            if (favoriteflg != 0) {
                favorite_button.text = getString(R.string.favorite_button_delete_label)
            } else {
                favorite_button.text = getString(R.string.favorite_button_entry_label)
            }
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
            progressBar.visibility = View.GONE
            favoriteflg = 0
            favorite_button.text = getString(R.string.favorite_button_entry_label)
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.favorite_fail_label), Snackbar.LENGTH_LONG).show()
        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    override fun onResume() {
        super.onResume()
//        Log.d("Android", "onResume")
        val user = FirebaseAuth.getInstance().currentUser
        val dataBaseReference = FirebaseDatabase.getInstance().reference

        if (user == null) {
            favorite_button.visibility = View.GONE
        } else {
            favorite_button.visibility = View.VISIBLE
            mFavoritesUserRef = dataBaseReference.child(FavoritesPATH).child(user.uid)
            mFavoritesUserRef.addChildEventListener(mFavoriteEventListener)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser

        fab.setOnClickListener {
            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }


        val dataBaseReference = FirebaseDatabase.getInstance().reference

        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString())
            .child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)

        favorite_button.setOnClickListener {
            mFavoritesRef = dataBaseReference.child(FavoritesPATH).child(user!!.uid).child(mQuestion.questionUid)
            val data = HashMap<String, String>()
            if(favoriteflg != 0) {
                mFavoritesRef.setValue(null)     //削除
            }else{
                data["genre"] = mQuestion.genre.toString()
                mFavoritesRef.setValue(data, this)
            }
            // プログレスバーを表示する
            progressBar.visibility = View.VISIBLE
        }

    }

    override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
        progressBar.visibility = View.GONE

        if (databaseError == null) {
            favorite_button.text = getString(R.string.favorite_button_delete_label)
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.favorite_success_label), Snackbar.LENGTH_LONG).show()
        } else {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.favorite_firebase_fail_label), Snackbar.LENGTH_LONG).show()
        }

    }
}


