package jp.techacademy.takaomi.okabe.qa_app


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

// findViewById()を呼び出さずに該当Viewを取得するために必要となるインポート宣言
import kotlinx.android.synthetic.main.activity_favorite.*

class Favorite : AppCompatActivity() {

    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter

    private var mFavoritesUserRef: DatabaseReference? = null
    private var mGenreQidRef: DatabaseReference? = null

    private val mFavoriteEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {

            val map = dataSnapshot.value as Map<String, String>
            val quid = dataSnapshot.key ?: ""
            val genre = map["genre"] ?: ""

            mDatabaseReference = FirebaseDatabase.getInstance().reference
            mGenreQidRef = mDatabaseReference.child(ContentsPATH).child(genre).child(quid)
//            mGenreRef!!.addChildEventListener(mQuestionEventListener)
            mGenreQidRef!!.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val map = dataSnapshot.value as Map<String, String>
                    val title = map["title"] ?: ""
                    val body = map["body"] ?: ""
                    val name = map["name"] ?: ""
                    val uid = map["uid"] ?: ""
                    val imageString = map["image"] ?: ""
                    val bytes =
                        if (imageString.isNotEmpty()) {
                            Base64.decode(imageString, Base64.DEFAULT)
                        } else {
                            byteArrayOf()
                        }

                    val answerArrayList = ArrayList<Answer>()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            answerArrayList.add(answer)
                        }
                    }

                    val question = Question(
                        title, body, name, uid, dataSnapshot.key ?: "",
                        genre.toInt(), bytes, answerArrayList
                    )

                    mQuestionArrayList.add(question)
                    mAdapter.notifyDataSetChanged()

                }

                override fun onCancelled(firebaseError: DatabaseError) {}
            })

        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(p0: DataSnapshot) {

        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {

        }

        override fun onCancelled(p0: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)
        title = getString(R.string.favorite_label)

        val user = FirebaseAuth.getInstance().currentUser

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        mFavoritesUserRef = mDatabaseReference.child(FavoritesPATH).child(user!!.uid)
        if (mFavoritesUserRef != null) {
            mFavoritesUserRef!!.addChildEventListener(mFavoriteEventListener)
        }

        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        favorite_listView.adapter = mAdapter

    }
}