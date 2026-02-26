package com.besome.sketch.tools;

import static com.besome.sketch.beans.QuizBean.QUIZ_ANSWER_A;
import static com.besome.sketch.beans.QuizBean.QUIZ_TRUE;

import android.app.Activity;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;

import com.besome.sketch.beans.QuizBean;

import java.util.ArrayList;
import java.util.Random;

import pro.sketchware.core.UIHelper;
import pro.sketchware.core.CompileQuizManager;
import pro.sketchware.R;
import pro.sketchware.databinding.QuizBoardBinding;

public class QuizBoard extends LinearLayout implements View.OnClickListener {

    private ArrayList<QuizBean> quizList;
    private QuizBean quizBean;
    private QuizCountDownTimer countdownTimer;
    private QuizBoardBinding quizBinding;

    public QuizBoard(Context context) {
        super(context);
        initialize(context);
    }

    private void setData(QuizBean quizBean) {
        this.quizBean = quizBean;
        quizBinding.tvQuestion.setText(quizBean.question);
        if (quizBean.type == 1) {
            quizBinding.layoutAnswerOx.setVisibility(View.VISIBLE);
            quizBinding.imgAnswerO.setVisibility(View.VISIBLE);
            quizBinding.imgAnswerX.setVisibility(View.VISIBLE);
            UIHelper.setSaturation(quizBinding.imgAnswerO, 1);
            UIHelper.setSaturation(quizBinding.imgAnswerX, 1);
            quizBinding.imgAnswerO.setOnClickListener(this);
            quizBinding.imgAnswerX.setOnClickListener(this);
            quizBinding.layoutAnswerAb.setVisibility(View.GONE);
        } else if (quizBean.type == 2) {
            quizBinding.layoutAnswerAb.setVisibility(View.VISIBLE);
            quizBinding.viewAnswerA.setOnClickListener(this);
            quizBinding.viewAnswerB.setOnClickListener(this);
            quizBinding.tvAnswerA.setText(quizBean.answerA);
            quizBinding.tvAnswerB.setText(quizBean.answerB);
            quizBinding.layoutAnswerOx.setVisibility(View.GONE);
        }
    }

    private void setTimeoutProgress(int elapsedMs) {
        int filledCount = elapsedMs / 250;
        int childCount = quizBinding.timeoutBar.getChildCount();

        while (true) {
            --childCount;
            if (childCount < filledCount) {
                return;
            }

            quizBinding.timeoutBar.getChildAt(childCount).setBackgroundColor(0xffeeeeee);
        }
    }

    public void cancelTimer() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
    }

    private void initialize(Context context) {
        quizBinding = QuizBoardBinding.inflate(((Activity) context).getLayoutInflater(), this, true);
        startQuiz();
    }

    public void loadNextQuestion() {
        if (quizList == null || quizList.isEmpty()) {
            quizList = CompileQuizManager.getQuizQuestions();
        }

        int randomIndex = new Random().nextInt(quizList.size());
        setData(quizList.remove(randomIndex));
        startTimer();
    }

    public final void invalidateClickListeners() {
        quizBinding.imgAnswerO.setOnClickListener(null);
        quizBinding.imgAnswerX.setOnClickListener(null);
        quizBinding.viewAnswerA.setOnClickListener(null);
        quizBinding.viewAnswerB.setOnClickListener(null);
    }

    public final void resetQuizViews() {
        quizBinding.imgAnswerO.setVisibility(View.VISIBLE);
        quizBinding.imgAnswerX.setVisibility(View.VISIBLE);
        quizBinding.separator.setVisibility(View.VISIBLE);

        quizBinding.imgAnswerA.setVisibility(View.GONE);
        quizBinding.imgAnswerB.setVisibility(View.GONE);
    }

    private void setXOResult(int answer) {
        if (answer == QUIZ_TRUE) {
            UIHelper.setSaturation(quizBinding.imgAnswerO, 1);
            UIHelper.setSaturation(quizBinding.imgAnswerX, 0);
            quizBinding.imgAnswerX.setVisibility(View.GONE);
        } else {
            UIHelper.setSaturation(quizBinding.imgAnswerO, 0);
            UIHelper.setSaturation(quizBinding.imgAnswerX, 1);
            quizBinding.imgAnswerO.setVisibility(View.GONE);
        }
        quizBinding.separator.setVisibility(View.GONE);
    }

    private void setABResult(int answer) {
        if (answer == QUIZ_ANSWER_A) {
            quizBinding.imgAnswerA.setVisibility(View.VISIBLE);
        } else {
            quizBinding.imgAnswerB.setVisibility(View.VISIBLE);
        }
    }

    public final void startTimer() {
        QuizCountDownTimer timer = countdownTimer;
        if (timer != null) {
            timer.cancel();
        }

        countdownTimer = null;
        timer = new QuizCountDownTimer(15000L, 250L);
        countdownTimer = timer;
        timer.start();
    }

    public void showAnswer() {
        QuizBean quizBean = this.quizBean;
        int quizType = quizBean.type;

        if (quizType == 1) {
            setXOResult(quizBean.answer);
        } else {
            setABResult(quizBean.answer);
        }

        invalidateClickListeners();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            resetQuizViews();
            loadNextQuestion();
        }, 2000); // ask next question after 2 secs
    }

    public void startQuiz() {
        resetQuizViews();
        loadNextQuestion();
    }

    @Override
    public void onClick(View view) {
        if (!UIHelper.isClickThrottled()) {
            cancelTimer();
            int id = view.getId();
            if (id == R.id.img_answer_o || id == R.id.img_answer_x || id == R.id.view_answer_a || id == R.id.view_answer_b) {
                showAnswer();
            }
            if (quizBean.type == 2) {
                view.getId();
            }
        }
    }

    public class QuizCountDownTimer extends CountDownTimer {
        public QuizCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            showAnswer();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            new Handler(Looper.getMainLooper()).post(() -> quizBinding.tvRemaingTime.setText(String.valueOf(millisUntilFinished / 1000L + 1L)));
        }
    }
}
