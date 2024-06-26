package MultiMode;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MultiModeRoom implements Serializable {
    private static final long serialVersionUID = 1344949L;
    private final int GAME_STARTED = 1;
    private final int GAME_NOT_STARTED = 0;
    private final transient List<ObjectOutputStream> clientOutputStreams = new ArrayList<>();
    private transient RoomObserver observer;
    private final HashSet<Long> finishedUserSet = new HashSet<>();
    private final Queue<UserDistance> updateQueue = new LinkedList<>();
    UserDistance[] top3UserDistances;
    private int status = GAME_NOT_STARTED;
    private int finishCount = 0;
    private int id; // 룸 ID
    private List<MultiModeUser> userList; // 유저 정보
    private MultiModeUser roomOwner; // 방장
    private RoomCreateInfo roomCreateInfo; // 방 정보
    private String title; // 방 제목
    private double distance; // 목표 거리
    private int numRunners; // 제한 인원
    private LocalDateTime startTime; // 시작 시각
    private Duration duration; // 목표 시간(달리는 시간)

    public MultiModeRoom(int roomId, RoomCreateInfo roomCreateInfo) { // 유저가 방을 만들때
        userList = new ArrayList<MultiModeUser>();
        this.id = roomId;
        this.roomCreateInfo = roomCreateInfo;
        this.title = roomCreateInfo.getTitle();
        this.distance = roomCreateInfo.getDistance();
        this.startTime = roomCreateInfo.getStartTime();
        this.duration = roomCreateInfo.getDuration();
        this.numRunners = roomCreateInfo.getNumRunners();
    }

    public MultiModeRoom(MultiModeUser user) { // 유저가 방을 만들때
        userList = new ArrayList<MultiModeUser>();
        user.enterRoom(this);
        userList.add(user); // 유저를 추가시킨 후
        this.roomOwner = user; // 방장을 유저로 만든다.
    }

    public MultiModeRoom() {

    }
    public void registerObserver(RoomObserver observer) {
        this.observer = observer;
    }

    public void notifyObserver(Packet packet) {
        this.observer.update(packet);
    }

    public void addUser(MultiModeUser user) {
        user.enterRoom(this);
        userList.add(user);
    }

    public void enterUser(MultiModeUser user, ObjectOutputStream oos) throws IOException {
        addUser(user);
        Packet enterRoomPacket = new PacketBuilder()
                .buildProtocol(Protocol.ENTER_ROOM)
                .buildUser(user)
                .buildSelectedRoom(this)
                .getPacket();
        oos.reset();
        oos.writeObject(enterRoomPacket);
        oos.flush();

        Packet updateRoomPacket = new PacketBuilder()
                .buildProtocol(Protocol.UPDATE_ROOM)
                .buildUser(user)
                .buildSelectedRoom(this)
                .getPacket();
        notifyObserver(updateRoomPacket);
        addOutputStream(oos);

    }

    public int exitUser(MultiModeUser user) {
        user.exitRoom(this);
        MultiModeUser userToRemove = null;
        int index = -1;
        for (int i = 0; i < userList.size(); i++) {
            MultiModeUser muser = userList.get(i);
            if (muser.getId() == user.getId()) {
                userToRemove = muser;
                index = i;
                break;
            }
        }

        if (userToRemove != null) {
            userList.remove(userToRemove);
            if (top3UserDistances != null) {
                UserDistance[] newTop3UserDistance = new UserDistance[Math.min(3, userList.size())];
                int arrindex = 0;
                for (int i = 0; i < top3UserDistances.length; i++) {
                    if (userList.contains(top3UserDistances[i].getUser())) {
                        newTop3UserDistance[arrindex++] = top3UserDistances[i];
                    }
                }
                top3UserDistances = newTop3UserDistance;
            }
        }

        if (index != -1) {
            removeOutputStream(index);
        }

        if (userList.size() < 1) {
            RoomManager.removeRoom(this);
            return index;
        }

        if (this.roomOwner.equals(user)) {
            this.roomOwner = userList.get(0);
        }

        if(status == GAME_NOT_STARTED) {
            Packet exitRoomPacket = new PacketBuilder()
                    .buildProtocol(Protocol.EXIT_ROOM)
                    .buildUser(user)
                    .buildSelectedRoom(this)
                    .getPacket();
            notifyObserver(exitRoomPacket);
        }

        return index;
    }

    public void close() {
        for (MultiModeUser user : userList) {
            user.exitRoom(this);
        }
        this.userList.clear();
        this.userList = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MultiModeRoom multiModeRoom = (MultiModeRoom) o;

        return id == multiModeRoom.id;
    }

    public String getTitle() { // 방 이름을 가져옴
        return title;
    }

    public double getDistance() {
        return distance;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public int getNumRunners() {
        return numRunners;
    }

    public int getUserSize() { // 유저의 수를 리턴
        return userList.size();
    }

    public MultiModeUser getOwner() { // 방장을 리턴
        return roomOwner;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<MultiModeUser> getUserList() {
        return userList;
    }

    public void setUserList(List<MultiModeUser> userList) {
        this.userList = userList;
    }

    public MultiModeUser getRoomOwner() {
        return roomOwner;
    }

    public void setRoomOwner(MultiModeUser roomOwner) {
        this.roomOwner = roomOwner;
    }

    public Duration getDuration() {
        return duration;
    }

    public RoomCreateInfo getRoomCreateInfo() {
        return roomCreateInfo;
    }

    public List<ObjectOutputStream> getOutputStream() {
        return clientOutputStreams;
    }

    public void addOutputStream(ObjectOutputStream o) {
        clientOutputStreams.add(o);
    }

    public void removeOutputStream(int index) {
        clientOutputStreams.remove(index);
    }


    public boolean isRoomOwner(MultiModeUser user) {
        return user.getId() == roomOwner.getId();
    }

    public boolean isRoomFull() {
        return userList.size() >= numRunners;
    }

    public void updateDistance(UserDistance userDistance) { // 유저의 distance를 업데이트
        updateQueue.add(userDistance);
    }

    public UserDistance[] getTop3UserDistance() { // 탑3 유저 distance를 리턴
        if (updateQueue.size() >= userList.size()) {
            List<UserDistance> topUserDistances = new ArrayList<>();
            for (int i = 0; i < userList.size(); i++) {
                UserDistance userDistance = updateQueue.poll();
                if (userDistance != null) {
                    topUserDistances.add(userDistance);
                }
            }
            Collections.sort(topUserDistances, new Comparator<UserDistance>() {
                @Override
                public int compare(UserDistance user1, UserDistance user2) {
                    return Double.compare(user2.getDistance(), user1.getDistance());
                }
            });
            if (top3UserDistances == null) {
                top3UserDistances = new UserDistance[Math.min(3, topUserDistances.size())];
            }
            for (int i = 0; i < top3UserDistances.length; i++) {
                top3UserDistances[i] = topUserDistances.get(i);
            }

            return top3UserDistances;
        }
        return null;
    }

    public UserDistance[] getResultTop3UserDistances() {
        if (top3UserDistances != null) {
            return top3UserDistances;
        } else
            return null;
    }

    public void addFinishCount(MultiModeUser user) {
        if (!finishedUserSet.contains(user.getId())) {
            finishedUserSet.add(user.getId());
            finishCount++;
        }
    }

    public boolean checkGameFinished() {
        return finishCount == userList.size();
    }

    public void startGame() {
        status = GAME_STARTED;

        Packet startGamePacket = new PacketBuilder()
                .buildProtocol(Protocol.START_GAME)
                .buildSelectedRoom(this)
                .getPacket();
        notifyObserver(startGamePacket);
    }

    public boolean canUpdate() {
        return updateQueue.size() >= userList.size();
    }

    public ObjectOutputStream getRoomOwnerOos() {
        int index = -1;
        for (int i = 0; i < userList.size(); i++) {
            if (roomOwner.getId() == userList.get(i).getId()) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            return clientOutputStreams.get(index);
        } else
            return null;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "MultiModeRoom{" +
                "id=" + id +
                ", userList=" + userList +
                ", roomOwner=" + roomOwner +
                ", roomCreateInfo=" + roomCreateInfo +
                '}';
    }

}
