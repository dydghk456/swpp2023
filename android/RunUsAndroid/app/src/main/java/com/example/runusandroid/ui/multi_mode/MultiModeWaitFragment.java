package com.example.runusandroid.ui.multi_mode;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.runusandroid.R;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import MultiMode.MultiModeRoom;
import MultiMode.MultiModeUser;
import MultiMode.Packet;
import MultiMode.Protocol;

public class MultiModeWaitFragment extends Fragment {

    private MultiModeRoom selectedRoom; // MultiModeRoom 객체를 저장할 멤버 변수
    private TextView titleTextView;
    private TextView startTimeTextView;
    private TextView timeRemainingTextView;

    private ConstraintLayout waitingListBox;

    //SocketManager socketManager = SocketManager.getInstance();  // SocketManager 인스턴스를 가져옴


    private final Handler handler = new Handler();
    private final int updateTimeInSeconds = 1; // 1초마다 업데이트


    public MultiModeWaitFragment() {
        // 빈 생성자는 기본 생성자와 함께 필요합니다.
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // XML 레이아웃 파일을 inflate
        View view = inflater.inflate(R.layout.fragment_multi_room_wait, container, false);
        titleTextView = view.findViewById(R.id.multi_room_wait_title);
        startTimeTextView = view.findViewById(R.id.multi_room_wait_start_time);
        timeRemainingTextView = view.findViewById(R.id.time_remaining);
        waitingListBox = view.findViewById(R.id.waiting_list_box);

        selectedRoom = (MultiModeRoom) getArguments().getSerializable("room");
        // 여기에서 MultiModeRoom 객체(multiModeRoom)를 사용하여 필요한 작업 수행
        if (selectedRoom != null) {
            // MultiModeRoom 객체에 저장된 정보를 화면에 표시
            TextView titleTextView = view.findViewById(R.id.multi_room_wait_title);
            TextView startTimeTextView = view.findViewById(R.id.multi_room_wait_start_time);

            titleTextView.setText(selectedRoom.getTitle());
            startTimeTextView.setText(selectedRoom.getStartTime());
            List<MultiModeUser> userList = selectedRoom.getUserList();
            if (userList != null && !userList.isEmpty()) {
                for (MultiModeUser user : userList) {
                    addUserNameToWaitingList(user.getNickname());
                }
            }
            handler.postDelayed(updateTimeRunnable, updateTimeInSeconds * 1000);

        }else{
            Log.d("Response", "no multiroom object");

        }
        Button leaveButton = view.findViewById(R.id.leaveButton);
        leaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 떠나기 버튼을 눌렀을 때 실행할 동작 추가
                new ExitRoomTask().execute();
                NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.navigation_multi_mode);
            }
        });
        // 필요한 초기화 및 작업 수행

        return view;
    }

    private void addUserNameToWaitingList(String userName) {
        TextView userNameTextView = new TextView(getContext());
        userNameTextView.setId(View.generateViewId());
        userNameTextView.setText(userName);
        userNameTextView.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.round_name_background));
        userNameTextView.setPadding(10, 10, 10, 10);
        userNameTextView.setGravity(Gravity.CENTER);

        // 레이아웃 매개 변수 설정
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(
                200,  // 너비를 200dp로 설정
                200   // 높이를 200dp로 설정
        );
        userNameTextView.setLayoutParams(layoutParams);

        int childCount = waitingListBox.getChildCount();

        if (childCount < 9) { // 최대 9개까지만 추가
            waitingListBox.addView(userNameTextView);

            ConstraintSet set = new ConstraintSet();
            set.clone(waitingListBox);

            // 그리드의 열 수
            int columns = 3;

            // 행 및 열 계산
            int row = childCount / columns;
            int col = childCount % columns;

            // 왼쪽 상단에 배치되도록 설정
            set.connect(userNameTextView.getId(), ConstraintSet.TOP, waitingListBox.getId(), ConstraintSet.TOP, 50);
            set.connect(userNameTextView.getId(), ConstraintSet.START, waitingListBox.getId(), ConstraintSet.START, 50);

            if (row > 0) {
                // 두 번째 행부터 아래쪽으로 이동
                View previousRowView = waitingListBox.getChildAt(childCount - columns);
                set.connect(userNameTextView.getId(), ConstraintSet.TOP, previousRowView.getId(), ConstraintSet.BOTTOM, 50);
            }

            if (col > 0) {
                // 두 번째 열부터 오른쪽으로 이동
                View previousColView = waitingListBox.getChildAt(childCount - 1);
                set.connect(userNameTextView.getId(), ConstraintSet.START, previousColView.getId(), ConstraintSet.END, 50);
            }

            set.applyTo(waitingListBox);
        }
    }


    private class ExitRoomTask extends AsyncTask<Void, Void, Boolean> {
        Packet packet;

        @Override
        protected Boolean doInBackground(Void... voids) {
            Socket socket = null;
            try {
                socket = new Socket("10.0.2.2", 5001);

                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
//                ObjectOutputStream oos = socketManager.getOOS();
//                ObjectInputStream ois = socketManager.getOIS();
                MultiModeUser user = new MultiModeUser(1, "chocochip"); // Update this as needed
                Packet requestPacket = new Packet(Protocol.EXIT_ROOM, user, selectedRoom);
                oos.writeObject(requestPacket);
                oos.flush();


                Object firstreceivedObject = ois.readObject(); //server의 broadcastNewClientInfo를
                Object receivedObject = ois.readObject();
                if (receivedObject instanceof Packet) {
                    packet = (Packet) receivedObject;
                }

                return true;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return false;
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                try {
//                    socketManager.closeSocket();  // 소켓 닫기
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }

            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (success) {
                Log.d("SendPacket", "Packet sent successfully!");

            } else {
                Log.e("SendPacket", "Failed to send packet!");
            }
        }

    }

    private final Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            // 현재 시간 가져오기
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            String currentTime = dateFormat.format(new Date());

            // startTimeTextView에서 시작 시간 가져오기
            String startTime = startTimeTextView.getText().toString() + ":00";

            // 시간 형식을 파싱하고 남은 시간 계산
            try {
                Date currentDateTime = dateFormat.parse(currentTime);
                Date startTimeDateTime = dateFormat.parse(startTime);

                long timeDifferenceMillis = startTimeDateTime.getTime() - currentDateTime.getTime();

                // startTime이 현재 시간보다 앞선 경우
                if (timeDifferenceMillis <= 0) {
                    timeRemainingTextView.setText("시작까지 0분 0초 남음");
                    return;  // Runnable 종료
                }

                long secondsRemaining = timeDifferenceMillis / 1000;

                // 시간, 분으로 변환
                long hours = secondsRemaining / 3600;
                long minutes = (secondsRemaining % 3600) / 60;
                long seconds = secondsRemaining % 60;

                // "x시간 x분 남음" 형식으로 문자열 구성
                String remainingTime;
                if (hours > 0) {
                    remainingTime = String.format(Locale.getDefault(), "시작까지 %d시간 %d분 남음", hours, minutes);
                } else {
                    remainingTime = String.format(Locale.getDefault(), "시작까지 %d분 %d초 남음", minutes, seconds);
                }

                // 업데이트된 시간을 텍스트 뷰에 설정
                timeRemainingTextView.setText(remainingTime);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 1초마다 업데이트
            handler.postDelayed(this, 1000); // 이 부분이 명확하지 않아, 일반적으로 1000ms (즉, 1초) 간격으로 설정합니다.
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 타이머가 더 이상 필요하지 않을 때 핸들러를 제거합니다.
        handler.removeCallbacks(updateTimeRunnable);
    }
}
