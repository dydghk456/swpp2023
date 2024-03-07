import MultiMode.*;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class Server {
    private RoomManager roomManager = new RoomManager();
    private ObjectOutputStream oos;
    private static final int PORT = 5001;
    private static final String SERVER_IP = "0.0.0.0";

    public Server() {
        try {

            InetSocketAddress address = new InetSocketAddress(SERVER_IP, PORT);
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.bind(address);

            while (true) {
                Socket socket = serverSocket.accept();
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                Thread clientThread = new Thread(() -> handleClient(socket, oos));
                clientThread.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getServerIPAddress() {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            return localhost.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "Unknown";
        }
    }

    private void handleClient(Socket socket, ObjectOutputStream oos) {
        MultiModeUser connectedUser = null;
        MultiModeUser user = null;
        PacketBuilder packetBuilder = null;
        try {
            InputStream is = socket.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(is);
            while (true) {
                try {
                    Object data = ois.readObject();
                    if (data instanceof Packet) {
                        connectedUser = ((Packet) data).getUser();
                        if (user == null) {
                            user = connectedUser;
                        }

                        if (((Packet) data).getProtocol() == Protocol.ROOM_LIST) {
                            packetBuilder = new PacketBuilder()
                                    .buildProtocol(Protocol.ROOM_LIST)
                                    .buildRoomList(RoomManager.getRoomList());
                            Packet roomListPacket = packetBuilder.getPacket();
                            oos.reset();
                            oos.writeObject(roomListPacket);
                            oos.flush();
                        } else if (((Packet) data).getProtocol() == Protocol.CREATE_ROOM) {
                            RoomCreateInfo roomCreateInfo = ((Packet) data).getRoomCreateInfo();
                            MultiModeRoom selectedRoom = RoomManager.createRoom(user, roomCreateInfo, oos);
                            packetBuilder = new PacketBuilder()
                                    .buildProtocol(Protocol.CREATE_ROOM)
                                    .buildRoomList(RoomManager.getRoomList())
                                    .buildSelectedRoom(selectedRoom);
                            Packet createRoomPacket = packetBuilder.getPacket();
                            oos.reset();
                            oos.writeObject(createRoomPacket);
                            oos.flush();
                        } else if (((Packet) data).getProtocol() == Protocol.ENTER_ROOM) {
                            MultiModeRoom enteredRoom = RoomManager.getRoom(((Packet) data).getSelectedRoom().getId());
                            if (enteredRoom == null) {
                                packetBuilder = new PacketBuilder()
                                        .buildProtocol(Protocol.CLOSED_ROOM_ERROR);
                                Packet closedRoomPacket = packetBuilder.getPacket();
                                oos.reset();
                                oos.writeObject(closedRoomPacket);
                                oos.flush();
                            } else if (enteredRoom.isRoomFull()) {
                                packetBuilder = new PacketBuilder()
                                        .buildProtocol(Protocol.FULL_ROOM_ERROR);
                                Packet fullRoomPacket = packetBuilder.getPacket();
                                oos.reset();
                                oos.writeObject(fullRoomPacket);
                                oos.flush();
                            } else {
                                enteredRoom.enterUser(user, oos);
                            }
                        } else if (((Packet) data).getProtocol() == Protocol.EXIT_ROOM) {
                            MultiModeRoom exitRoom = RoomManager.getRoom(((Packet) data).getSelectedRoom().getId());
                            int index = exitRoom.exitUser(user);
                        } else if (((Packet) data).getProtocol() == Protocol.UPDATE_USER_DISTANCE) {
                            MultiModeRoom updateRoom = RoomManager.getInGameRoom(user.getRoom().getId());
                            Float distance = ((Packet) data).getDistance();
                            updateRoom.updateDistance(new UserDistance(user, distance));
                            if (updateRoom.canUpdate()) {
                                updateTop3Users(Protocol.UPDATE_TOP3_STATES, updateRoom);
                            }
                        } else if (((Packet) data).getProtocol() == Protocol.START_GAME) {
                            MultiModeRoom enteredRoom = RoomManager.getRoom(((Packet) data).getSelectedRoom().getId());
                            RoomManager.startRoom(enteredRoom);
                            enteredRoom.startGame();
                        } else if (((Packet) data).getProtocol() == Protocol.EXIT_GAME) {
                            MultiModeRoom exitRoom = RoomManager.getInGameRoom(((Packet) data).getSelectedRoom().getId());
                            int index = exitRoom.exitUser(user);
                        } else if (((Packet) data).getProtocol() == Protocol.FINISH_GAME) {
                            MultiModeRoom finishRoom = RoomManager.getInGameRoom(((Packet) data).getSelectedRoom().getId());
                            finishRoom.addFinishCount(user);
                            if (finishRoom.checkGameFinished()) {
                                sendResultToRoomOwner(Protocol.SAVE_GROUP_HISTORY, finishRoom);
                            }
                        } else if (((Packet) data).getProtocol() == Protocol.SAVE_GROUP_HISTORY) {
                            MultiModeRoom finishRoom = RoomManager.getInGameRoom(((Packet) data).getSelectedRoom().getId());
                            sendResultTop3Users(Protocol.CLOSE_GAME, finishRoom, ((Packet) data).getGroupHistoryId());
                            RoomManager.removeInGameRoom(finishRoom);
                        }
                    } else if (data instanceof String) {
                        System.out.println((String) data);
                    }

                    printRoomListInfo(RoomManager.getRoomList());
                } catch (SocketException | EOFException e) {
                    if (user != null && user.getRoom() != null) {
                        MultiModeRoom exitRoom = RoomManager.getRoom(user.getRoom().getId());
                        int index = exitRoom.exitUser(user);
                    }
                    break;
                }
            }
        } catch (SocketException e) {
            MultiModeUser currentUser = null;
            MultiModeRoom exitRoom = null;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            cleanupClientResources(oos, socket);
        }
    }

    private void updateTop3Users(int protocol, MultiModeRoom room) {
        UserDistance[] top3UserDistance = null;
        while (true) {
            top3UserDistance = room.getTop3UserDistance();
            if (top3UserDistance != null) {
                break;
            }
        }
        List<UserDistance> lTop3UserDistances = new ArrayList<UserDistance>(Arrays.asList(top3UserDistance));
        PacketBuilder packetBuilder = new PacketBuilder()
                .buildProtocol(protocol)
                .buildListTop3UserDistance(lTop3UserDistances);
        Packet updateTop3Packet = packetBuilder.getPacket();
        broadcastToRoomUsers(room, updateTop3Packet);
    }

    private void sendResultToRoomOwner(int protocol, MultiModeRoom room) throws IOException {
        UserDistance[] top3UserDistance = null;
        while (true) {
            top3UserDistance = room.getResultTop3UserDistances();
            if (top3UserDistance != null) {
                break;
            }
        }
        List<UserDistance> lTop3UserDistances = new ArrayList<UserDistance>(Arrays.asList(top3UserDistance));
        System.out.println(lTop3UserDistances);
        PacketBuilder packetBuilder = new PacketBuilder()
                .buildProtocol(protocol)
                .buildListTop3UserDistance(lTop3UserDistances);
        Packet updateTop3Packet = packetBuilder.getPacket();

        ObjectOutputStream oos = room.getRoomOwnerOos();
        if (oos != null) {
            try {
                oos.reset();
                oos.writeObject(updateTop3Packet);
                oos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendResultTop3Users(int protocol, MultiModeRoom room, long groupHistoryId) {
        UserDistance[] top3UserDistance = null;
        while (true) {
            top3UserDistance = room.getResultTop3UserDistances();
            if (top3UserDistance != null) {
                break;
            }
        }

        List<UserDistance> lTop3UserDistances = new ArrayList<UserDistance>(Arrays.asList(top3UserDistance));
        PacketBuilder packetBuilder = new PacketBuilder()
                .buildProtocol(protocol)
                .buildListTop3UserDistance(lTop3UserDistances)
                .buildGroupHistoryId((int) groupHistoryId);
        Packet updateTop3Packet = packetBuilder.getPacket();
        System.out.println((int) groupHistoryId);
        broadcastToRoomUsers(room, updateTop3Packet);
    }

    private void broadcastToRoomUsers(MultiModeRoom room, Packet packet) {
        List<ObjectOutputStream> oosList = room.getOutputStream();
        for (int i = 0; i < oosList.size(); i++) {
            ObjectOutputStream oos = oosList.get(i);
            if (oos != null) {
                try {
                    oos.reset();
                    oos.writeObject(packet);
                    oos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void cleanupClientResources(ObjectOutputStream oos, Socket socket) {
        try {
            oos.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printRoomListInfo(List<MultiModeRoom> roomList) {
        for (MultiModeRoom room : roomList) {
            printRoomInfo(room);
        }
    }

    private void printRoomInfo(MultiModeRoom room) {
        if (room == null) {
            System.out.println("room is null");
            return;
        } else {
            List<MultiModeUser> userList = room.getUserList();
            System.out.println(room.getTitle());
            for (MultiModeUser user : userList) {
                System.out.println("username : " + user.getNickname());
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        new Server();
    }

}