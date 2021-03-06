
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;


public class JhjServer 
{
	static {
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
		} catch(ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}
	}
	ServerSocket serverSocket = null;
	Socket socket = null;
	Map<String, PrintWriter> clientMap;
	String title; //방이름	
		

	Map<String, String> chatUserMap;		//채팅방에 들어가있는 인원  이름 방제목
	Map<String, Integer> roomInfoMap;		//전체방정보				방제목 인원수
	Map<String, PrintWriter> noWaitMap;		//채팅중인 인원정보			이름 출력		  
	Map<String, Integer> openInfoMap;		//공개방 정보				방제목 인원수
	
	//비공개방
	Map<String, String> secretUserMap;		//비공채팅방 입장인원		이름 방제목
	Map<String, Integer> pwInfoMap;			//비공개방 비번 정보		방제목 비밀번호
	Map<String, Integer> secretInfoMap;		//비공개방 방 정보			방제목 인원수
	
	Map<String, String> roomOwnerMap;		//방장 정보					방제목 이름
	Map<String, PrintWriter> guestMap;		//대기실인원정보			이름 출력
	
	Map<String, String> banListMap;			//영구강퇴 정보				이름 방제목
	Map<String, String> serverBanWordMap;	//서버금칙어 				단어 서버
	Map<String, String> privateBanWordMap;	//개인금칙어				단어 개인이름(나중엔 아이디로 변경지정해야됨)
	
	Map<String, BufferedReader> inviteInfoMap; //초대 대답					이름 입력
	
	//생성자
	public JhjServer() {
		//클라이언트의 출력스트림을 저장할 해쉬맵 생성
		clientMap = new HashMap<String, PrintWriter>();
		//해쉬맵 동기화 설정.
		Collections.synchronizedMap(clientMap);
		
		inviteInfoMap = new HashMap<String, BufferedReader>();
		Collections.synchronizedMap(inviteInfoMap);
		
		chatUserMap = new HashMap<String, String>();
		Collections.synchronizedMap(chatUserMap);
		roomInfoMap = new HashMap<String, Integer>();
		Collections.synchronizedMap(roomInfoMap);
		noWaitMap = new HashMap<String, PrintWriter>();
		Collections.synchronizedMap(noWaitMap);
		openInfoMap = new HashMap<String, Integer>();
		Collections.synchronizedMap(openInfoMap);
		
		secretUserMap = new HashMap<String, String>();
		Collections.synchronizedMap(secretUserMap);
		pwInfoMap = new HashMap<String, Integer>();
		Collections.synchronizedMap(pwInfoMap);
		secretInfoMap = new HashMap<String, Integer>();
		Collections.synchronizedMap(secretInfoMap);
		
		roomOwnerMap = new HashMap<String, String>();
		Collections.synchronizedMap(roomOwnerMap);
		guestMap = new HashMap<String, PrintWriter>();
		Collections.synchronizedMap(guestMap);
		banListMap = new HashMap<String, String>();
		Collections.synchronizedMap(banListMap);
		serverBanWordMap = new HashMap<String, String>();
		Collections.synchronizedMap(serverBanWordMap);
		privateBanWordMap = new HashMap<String, String>();
		Collections.synchronizedMap(privateBanWordMap);
		
		serverBanWordMap.put("hello", "server");
		privateBanWordMap.put("hi","aaaa");
		
	}
	
	public void init()
	{
		try {
			serverSocket = new ServerSocket(9999); //9999포트로 서버소켓 객체 생성.
			System.out.println("서버가 시작되었습니다...");
			
			while(true) {
				socket = serverSocket.accept();
				System.out.println(socket.getInetAddress() + ":" + socket.getPort());
				
				Thread mst = new MultiServerT(socket); //쓰레드 생성
				mst.start(); // 쓰레드 시동
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				serverSocket.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}		
	}
	
	//접속자 리스트 보내기
	public void list(PrintWriter out)
	{
		//출력스트림을 순차적으로 얻어와서 해당메시지를 출력한다.
		Iterator<String> it = clientMap.keySet().iterator();
		String msg = "전체사용자 리스트 [";
		while(it.hasNext()) {
			msg += (String)it.next() + ",";
		}
		msg = msg.substring(0, msg.length()-1) + "]";
		out.println(msg);
		
		Iterator<String> it2 = guestMap.keySet().iterator();
		msg = "대기실 사용자 리스트 [";
		while(it2.hasNext()) {
			msg += (String)it2.next() + ",";
		}
		msg = msg.substring(0, msg.length()-1) + "]";
		out.println(msg);
	}
	
	public String msgCheck(String name, String s)  //금칙어 체크 메서드
	{
		PrintWriter out = clientMap.get(name);
		if(serverBanWordMap.containsKey(s))
		{
			String alert = serverBanWordMap.get(s);
			out.println("해당단어는 서버 금칙어입니다.");
			s = "서버 금칙어";
			return s;
		}
		else if(privateBanWordMap.containsKey(s))
		{
			out.println("해당 단어는 개인 금칙어입니다.");
			s = "개인 금칙어";
			return s;
		}
		else
		{
			return s;
		}
	}
	
	//귓속말
	public void sendTo(String name, String s)
	{	
		msgCheck(name, s);
		int start = s.indexOf(" ")+1;
		int end = s.indexOf(" ",start);
		if (end != -1) {
			String toname = s.substring(start,end);
			String msg = s.substring(end+1);
			Object obj = clientMap.get(toname);
			if(obj != null) {
				PrintWriter to_out = (PrintWriter)obj;
				to_out.println("["+name+"]"+"->"+ "["+toname+"] "+msg);
				to_out.flush();
			}
		}
	}
	
	public void guestMsg(String name, String s)
	{	
		msgCheck(name, s);
		Iterator<String> it = guestMap.keySet().iterator();
		
		while(it.hasNext()) {
			try {
				PrintWriter it_out = (PrintWriter) guestMap.get(it.next());
				if(name.equals(""))
					it_out.println(s);
				else
					it_out.println("["+name+"] "+s);
			}catch(Exception e) {
				System.out.println("예외:"+e);
			}
		}
	}
	
	//접속된 모든 클라이언트들에게 메시지를 전달.
	public void sendAllMsg(String user, String msg)//공지 사항으로 변경 해도 될듯
	{
		msgCheck(user, msg);
		//출력스트림을 순차적으로 얻어와서 해당 메시지를 출력한다.
		Iterator<String> it = clientMap.keySet().iterator();
		
		while(it.hasNext()) {
			try {
				PrintWriter it_out = (PrintWriter) clientMap.get(it.next());
				if(user.equals(""))
					it_out.println(msg);
				else
					it_out.println("["+user+"] "+msg);
			}catch(Exception e) {
				System.out.println("예외:"+e);
			}
		}
	}
	
//=======================================대기실메뉴=======================================//
	public void waitingRoomMenu(String name)
	{
		Object obj = clientMap.get(name);
		PrintWriter out = (PrintWriter)obj;
		
		out.println("/공개방만들기			: /makeroom");
		out.println("/비공개방 만들기		: /secretroom");
		out.println("/전체방리스트보기		: /roomlist");
		out.println("/공개방리스트보기		: /openlist");
		out.println("/비 공개방리스트보기	: /secretlist");
		out.println("/대화방참여			: /enter ");
	}
	// 공개방만들기
	public void makeRoom(String name,String title,int number, PrintWriter out)
	{			
		chatUserMap.put(name, title);
		roomInfoMap.put(title, number);
		openInfoMap.put(title, number);
		
		noWaitMap.put(name, out);
		guestMap.remove(name);		
		roomOwnerMap.put(title, name);
		
		out.println("공개채팅방 "+title+"가 개설, 입장하였습니다.");
//		Set set=chatUserMap.entrySet();
//		System.out.println(set);
//		System.out.println("ㅡㅡㅡㅡz");
	}
	///비공개방만들기
	public void secretroom(String name,String title,int number,int pw, PrintWriter out)
	{
		chatUserMap.put(name, title);
		roomInfoMap.put(title, number);
		secretInfoMap.put(title, number);
		pwInfoMap.put(title, pw);
		
		noWaitMap.put(name, out);
		guestMap.remove(name);	
		roomOwnerMap.put(title, name);	
		
		out.println("비공개채팅방 "+title+"가 개설, 입장하였습니다.");
	}
	
	//채팅방 입장 후에 채팅 메서드
	public void sendChatRoom(String name, String title, String s)
	{
		msgCheck(name, s);
		System.out.println("ㅡㅡㅡㅡ5");
		Iterator<String> itr = chatUserMap.keySet().iterator();
		while(itr.hasNext())
		{
			System.out.println("ㅡㅡㅡㅡ55");
			String rn = itr.next();
			if(chatUserMap.get(rn).equals(title))
			{
				System.out.println("ㅡㅡㅡㅡ555");
				Object obj = noWaitMap.get(rn);
				PrintWriter out = (PrintWriter)obj;
				out.println("["+name+"] : "+s);
				}
			else 
			{
					System.out.println("ㅡㅡㅡㅡ5555");
					System.out.println(s);
			}
		}
	}
//			if(chatUserMap.containsValue(title))
//			{
//				//String to_name = (String)(itr.next());
//				Object obj = noWaitMap.get(itr.next());
//				PrintWriter out = (PrintWriter)obj;
//				out.println("["+name+"] : "+s);
//			}else {
//				System.out.println(s);
//			}
	
	
//====================================================================================//
//====================================================================================//	
	
	/////////////////////////////////// 전체방리스트출력
	public void roomlist(PrintWriter out)
	{
//		Iterator<String> it = roomInfoMap.keySet().iterator();
//		String msg = "전체 방 리스트 [";
//		while(it.hasNext()){
//			msg += (String)it.next() + ","; 
//		}
//		msg = msg.substring(0, msg.length()-1) + "]";
//		out.println(msg);
		if(roomInfoMap.size() > 0) {
			Set<String> keyset = roomInfoMap.keySet();
			out.println(keyset);
		}else {
			out.println("방이 없습니다.");
		}
	
	}
	public void openlist(PrintWriter out) //공방리스트
	{
		if(openInfoMap.keySet() != null) {
			Set keyset = openInfoMap.keySet();
			out.println(keyset);
		}else {
			out.println("방이 없습니다.");
		}
	}
	public void secretlist(PrintWriter out) //비방리스트
	{
		if(secretInfoMap.keySet() != null) {
			Set keyset = secretInfoMap.keySet();
			out.println(keyset);
		}else {
			out.println("방이 없습니다.");
		}
	}
	
	/////////////////////////////////////// 공개방입장
	public void enterRoom(String name, String title, PrintWriter out)
	{
		int count = openInfoMap.get(title);
		
		Iterator itr = chatUserMap.keySet().iterator();
		int i = 0;
		while(itr.hasNext())
		{
			String roomName = chatUserMap.get(itr.next());
			if(title.equals(roomName))
			{
				i = i+1;
			}
		}
		if(i<count)
		{
			chatUserMap.put(name, title);
			noWaitMap.put(name, out);
			guestMap.remove(name, out);
			out.println(title+" 방에 입장");
			String s = "입장하셧습니다.";
			sendChatRoom(name, title, s);
		}else {
			out.println("정원초과");
		}
		
	}
	////// 비공개방입장
	public void enterSecretRoom(String name, String title, PrintWriter out)
	{
		System.out.println("ㅡㅡㅡㅡㅁ");
		int count = secretInfoMap.get(title);		
		Iterator<String> itr = chatUserMap.keySet().iterator();
		int i = 0;
		System.out.println("ㅡㅡㅡㅡㄴ");
		while(itr.hasNext())
		{
			System.out.println("ㅡㅡㅡㅡㄹ");
			String roomName = chatUserMap.get(itr.next());
			if(title.equals(roomName))
			{
				i = i+1;
			}
		}
		if(i<count)
		{
			chatUserMap.put(name, title);
			noWaitMap.put(name, out);
			guestMap.remove(name, out);
			out.println(title+" 방에 입장");
			String s = "입장하셧습니다.";
			sendChatRoom(name, title, s);
		}else {
			out.println("정원초과");
		}					
	}
//=====================================대기실메뉴=====================================//
	
//=====================================채팅방메뉴=====================================//
	public void chatRoomMenu(PrintWriter out)
	{
		out.println("채팅방 내 메뉴");
		out.println("대화방 리스트 보기			:	/wholeroomlist");
		out.println("대기실 사용자 리스트 보기	:	/waituserlist");
		out.println("내 룸 사용자리스트보기		:	/myroomlist");
		out.println("초대하기 -방장만 가능 		:	/invite");
		out.println("강퇴		-일회성 강퇴	:	/kick");
		out.println("			-영구성 강퇴	:	/ban");
		out.println("룸나가기					:	/exit");
		out.println("방장승계					:	/succession");
		out.println("방폭파						:	/close");
	}
	public void wholeRoomList(PrintWriter out)
	{
		if(roomInfoMap.size() > 0 ) {
			Set<String> keyset = roomInfoMap.keySet();
			out.println(keyset);
		}else {
			out.println("방이 없습니다.");
		}
	}
	public void waitUserList(PrintWriter out)
	{
		if(guestMap.size() > 0) {
			Set<String> keyset = guestMap.keySet();
			out.println(keyset);
		}else {
			out.println("대기실 유저 없다");
		}
	}
	public void myRoomList(String name, String title,PrintWriter out)
	{
		Iterator<String> itr = chatUserMap.keySet().iterator();
		while(itr.hasNext())
		{
			String chatUsername = itr.next();
			if(title.equals(chatUserMap.get(chatUsername)))
			{
				out.println(chatUsername);
			}else {
				System.out.println("mrl조건오류");
			}
		}
	}
	public void inviteCheck(String name, String man, String answer)//초대한사람, 초대받은 사람, 받은사람의대답
	{
		
		if(roomOwnerMap.containsValue(name) && answer.equalsIgnoreCase("y"))
		{
			System.out.println("-------3");
			title = chatUserMap.get(name);
			PrintWriter out = (PrintWriter)clientMap.get(man);
			Invite(man, title, out);
		}
		else
		{
			PrintWriter out = (PrintWriter)clientMap.get(name);
			out.println("방장이 아니라 초대 불가능 or 상대방 거부");
		}
	}
	public void Invite(String name, String title, PrintWriter out) //초대받은이름, 방제, 초대받은사람 out
	{
		if(openInfoMap.containsKey(title)) //공방입장
		{
			System.out.println("-------4");
			enterRoom(name, title, out);
		}
		else if(secretInfoMap.containsKey(title))//비방입장
		{
			System.out.println("-------5");
			enterSecretRoom(name, title, out);
		}
	}
	public void kickCheck(String name, String title, PrintWriter out, String kick, PrintWriter to_out)
	{//방장이름, 방제목, 방장출력스트림, 강퇴할사람 이름, 강퇴할사람 출력스트림
		if(roomOwnerMap.containsValue(name))
		{
			if( title.equals(chatUserMap.get(kick)) )
			{
				Kick(name, title, out, kick, to_out);
			}
			else
			{
				out.println("대상 없음");
			}
		}
		else
		{
			System.out.println("-------3");
			out.println("방장이 아니라 안됨");
		} 
	}
	public void Kick(String name, String title, PrintWriter out, String kick, PrintWriter to_out)
	{//방장, 방제목, 방장출력스트림, 강퇴할사람 이름, 강퇴할사람 출력스트림
		chatUserMap.remove(kick);
		noWaitMap.remove(kick);
		secretUserMap.remove(kick);
		
		guestMap.put(kick, to_out);
		
		out.println(title+"방에서"+kick+"님이 강퇴당해서 퇴장");
		to_out.println(name+"님에 의해 강퇴당함");
		String s = "강퇴 되었습니다.";
		sendChatRoom(name, title, s);
	}
	public void banCheck(String name, String title, PrintWriter out, String kick, PrintWriter to_out)
	{//방장, 방제목, 방장출력스트림, 강퇴할사람 이름, 강퇴할사람 출력스트림
		if(roomOwnerMap.containsValue(name))
		{
			if( title.equals(chatUserMap.get(kick)) )
			{
				Ban(name, title, out, kick, to_out);
			}
			else
			{
				out.println("대상이 없습니다.");
			}
		}
		else
		{
			System.out.println("-------3");
			out.println("방장이 아니라 권한이 없습니다.");
		} 
	}
	public void Ban(String name, String title, PrintWriter out, String kick, PrintWriter to_out)
	{//방장이름, 방제목, 방장 출력스트림, 영구강퇴대상이름, 대상자 출력스트림
		chatUserMap.remove(kick);
		noWaitMap.remove(kick);
		secretUserMap.remove(kick);
		
		guestMap.put(kick, to_out);
		banListMap.put(kick, title);//해당 제목 방에서 대상자 영구 강퇴 맵에 정보 등록
		
		out.println(title+"방에서"+kick+"님이 영구강퇴당해서 퇴장");
		to_out.println(name+"님에 의해 영구강퇴당함");
		String s = "영구강퇴 되었습니다.";
		sendChatRoom(name, title, s);
	}
//	public void exitRoom(String name, String title, PrintWriter out)
//	{
//		chatUserMap.remove(name);
//		noWaitMap.remove(name);
//		secretUserMap.remove(name);
//		
//		guestMap.put(name, out);
//	}
	public void openRoomExit(String name, String title, PrintWriter out)
	{//유저 이름, 방제목, 출력스트림
		System.out.println("--------ox1");
		chatUserMap.remove(name);
		noWaitMap.remove(name);
		guestMap.put(name, out);
		
		Iterator<String> itrRn = chatUserMap.keySet().iterator();
		int num = 0;
		while(itrRn.hasNext())
		{
			System.out.println("ㅡㅡㅡㅡㄹ");
			String roomName = chatUserMap.get(itrRn.next());
			if(title.equals(roomName))
			{
				num = num+1;
			}
		}
		//이미 해당인원 나간상황
		//먼저 인원체크 1명이면 방장이 남을 수 밖에 없음. 방장이 퇴장하면 0명 되서 방사라짐.
		//2명에서 한명 나갈때, 방장이 나갈때, 비 방장이 나갈때.
		if(num >= 1)//방제 인원남아있을때
		{
			if(roomOwnerMap.containsValue(name))
			{
				System.out.println("--------ox2");
				roomOwnerMap.remove(title);
				
				System.out.println("--------ox3");
				String s = "이 방에서 퇴장.";
				sendChatRoom(name, title, s);
				Iterator<String> itr = chatUserMap.keySet().iterator();
				String ownerName = itr.next();
				roomOwnerMap.put(title, ownerName);
				//방장 변경
				s = "으로 방장 변경.";
				sendChatRoom(ownerName, title, s);
				Set set = roomOwnerMap.entrySet();
				System.out.println(set);
				
			}
			else
			{
				System.out.println("--------ox5");
				String s = "이 방에서 퇴장.";
				sendChatRoom(name, title, s);
			}
			
		}
		else//방에 남은인원이 없을때
		{
			roomInfoMap.remove(title);
			openInfoMap.remove(title);
			roomOwnerMap.remove(title);
		}
		
	}
	public void secretRoomExit(String name, String title, PrintWriter out)
	{
		System.out.println("--------s x");
		chatUserMap.remove(name);	
		noWaitMap.remove(name);
		secretUserMap.remove(name);
		
		guestMap.put(name, out);
		
		Iterator<String> itrRn = chatUserMap.keySet().iterator();
		int num = 0;
		while(itrRn.hasNext())
		{
			System.out.println("ㅡㅡㅡㅡㄹ");
			String roomName = chatUserMap.get(itrRn.next());
			if(title.equals(roomName))
			{
				num = num+1;
			}
		}
		
		if(num >= 1)//방제 인원남아있을때
		{
			if(roomOwnerMap.containsValue(name))
			{
				System.out.println("--------ox2");
				roomOwnerMap.remove(title);
				
				System.out.println("--------ox3");
				String s = "이 방에서 퇴장.";
				sendChatRoom(name, title, s);
				Iterator<String> itr = chatUserMap.keySet().iterator();
				String ownerName = itr.next();
				roomOwnerMap.put(title, ownerName);
				//방장 변경
				s = "으로 방장 변경.";
				sendChatRoom(ownerName, title, s);
				Set set = roomOwnerMap.entrySet();
				System.out.println(set);
				
			}
			else
			{
				System.out.println("--------ox5");
				String s = "이 방에서 퇴장.";
				sendChatRoom(name, title, s);
			}
			
		}
		else//방에 남은인원이 없을때
		{
			roomInfoMap.remove(title);
			secretInfoMap.remove(title);
			pwInfoMap.remove(title);
			secretInfoMap.remove(title);
			roomOwnerMap.remove(title);
		}
	}
	
	public void successionOwner(String name, String title, String man)
	{//방장 이름, 방제목, 승계할 대상이름
		Iterator<String> itr = chatUserMap.keySet().iterator();
		if( title.equals(chatUserMap.get(man)) )
		{
			roomOwnerMap.remove(title);
			roomOwnerMap.put(title, man); //방장 지칭된 대상으로 승계 
			
			String s = "이 방장이 되었습니다.";
			sendChatRoom(man, title, s);	
		}else
		{
			PrintWriter out = (PrintWriter)clientMap.get(name);
			out.println("대상을 잘못 지정했습니다.");
		}
		
	}
	public void closepublicRoom(String name, String title, PrintWriter out) 
	{
		if(!roomOwnerMap.containsValue(name))
		{
			out.println("권한 없음.");
		}
		else
		{
			chatUserMap.remove(name);
			roomOwnerMap.remove(name);
			noWaitMap.remove(name);
			guestMap.put(name, out);
			Iterator<String> itr = chatUserMap.keySet().iterator();
			System.out.println("ㅡㅡㅡㅡㅡ10");
			while(chatUserMap.containsValue(title))
			{
				System.out.println("ㅡㅡㅡㅡㅡ11");
				String rn = itr.next();
				System.out.println(rn);
				if(title.equals(chatUserMap.get(rn)))
				{				
					System.out.println("ㅡㅡㅡㅡㅡ12");
					chatUserMap.remove(rn);
//					roomInfoMap.remove(title);
//					openInfoMap.remove(title);
					roomOwnerMap.remove(rn);
					noWaitMap.remove(rn);
					System.out.println("ㅡㅡㅡㅡㅡ121");
					Object obj = clientMap.get(rn);
					PrintWriter to_out = (PrintWriter)obj;
					System.out.println("ㅡㅡㅡㅡㅡ122");
					guestMap.put(rn, to_out);
					System.out.println("ㅡㅡㅡㅡㅡ123");
					out.println(title+"방을닫았습니다.");
					to_out.println(name+"님이"+title+" 방을 닫았습니다.");
				}
				
			}		
			roomInfoMap.remove(title);
			openInfoMap.remove(title);
		}
	}
	public void closeSecretRoom(String name, String title, PrintWriter out)
	{
		if(!roomOwnerMap.containsValue(name))
		{
			out.println("권한 없음.");
		}
		else
		{
			chatUserMap.remove(name);
			secretUserMap.remove(name);
			roomOwnerMap.remove(name);
			noWaitMap.remove(name);
			guestMap.put(name, out);
			Iterator<String> itr = chatUserMap.keySet().iterator();
			System.out.println("ㅡㅡㅡㅡㅡ10");
			String rn;
			while(chatUserMap.containsValue(title))
			{
				
				System.out.println("ㅡㅡㅡㅡㅡ11");
				rn = itr.next();
				System.out.println(rn);
				if(title.equals(chatUserMap.get(rn)))
				{				
					System.out.println("ㅡㅡㅡㅡㅡ12");
					chatUserMap.remove(rn);
					secretUserMap.remove(name);
//					roomInfoMap.remove(title);
//					openInfoMap.remove(title);
					roomOwnerMap.remove(rn);
					noWaitMap.remove(rn);
					System.out.println("ㅡㅡㅡㅡㅡ121");
					Object obj = clientMap.get(rn);
					PrintWriter to_out = (PrintWriter)obj;
					System.out.println("ㅡㅡㅡㅡㅡ122");
					guestMap.put(rn, to_out);
					System.out.println("ㅡㅡㅡㅡㅡ123");
					out.println(title+"방을닫았습니다.");
					to_out.println(name+"님이" + title +" 방을 닫았습니다.");
				}
				
			}		
			roomInfoMap.remove(title);
			secretInfoMap.remove(title);
			pwInfoMap.remove(title);
		}
	}
	
//=========================================================================//	
//=========================================================================//	
	
	
	public static void main(String[] args) 
	{	
		
		//서버객체 생성
		JhjServer ms = new JhjServer();
		ms.init();

	}
	
///////////////////////////////////////////////////////////////////////
//내부 클래스
//클라이언트로부터 읽어온 메시지를 다른 클라이언트(socket)에 보내는 역할을 하는 메서드
	
	class MultiServerT extends Thread
	{
		Socket socket;
		PrintWriter out = null;
		BufferedReader in = null;
		
		//생성자
		public MultiServerT(Socket socket) {
			this.socket = socket;
			try {
				out = new PrintWriter(this.socket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(this.socket.getInputStream() ));
							
			} catch(Exception e) {
				System.out.println("예외:" + e);
			}
		}
		
		//쓰레드를 사용하기 위해서 run()메서드 재정의
		@Override
		public void run()
		{
			//String s = "";
			String name = ""; //클라이언트로부터 받은 이름을 저장할 변수
			try {
				name = in.readLine(); //클라이언트에서 처음으로 보내는 메시지는
				  //클라이언트가 사용할 이름이다.
				
				sendAllMsg("", name +"님이 대기실에 입장하셨습니다.");
				//현재 객체가 가지고있는 소켓을 제외하고 다른 소켓(클라이언트)들에게 접속알림
				clientMap.put(name, out);//해쉬맵에 키를 name으로 출력스트림 객체를 저장
				guestMap.put(name, out);//게스트 운영하는 맵
				inviteInfoMap.put(name, in);//초대 관련 버퍼리더 정보 맵
				System.out.println("현재 접속자 수는"+clientMap.size()+"명 입니다.");
				
				//입력스트림이 null이 아니면 반복
				String s = "";
				while(in != null) {
					s = in.readLine();
					System.out.println(s);
					StringTokenizer tokens = new StringTokenizer(s.substring(1));
//					if( s.equals("q") || s.equals("Q") )
//						break;
//					if ( s.equals("/list")) //list 출력
//						list(out);				
					
					System.out.println("ㅡㅡㅡㅡa");
					if (chatUserMap.containsKey(name))
					{
						if ( s.substring(0, 1).equals("/"))
						{
							StringTokenizer str = new StringTokenizer(s.substring(1));
							String order = str.nextToken();
							System.out.println("ㅡㅡㅡㅡaa");
							
//							Object obj = clientMap.get(name);
//							PrintWriter out = (PrintWriter)obj;
							if(order.equals("to"))	
							{	sendTo(name, s);	}
							else if(order.equals("roommenu"))
							{
								System.out.println("--tt");
								chatRoomMenu(out);
							}
							else if(order.equals("wholeroomlist"))
							{
								wholeRoomList(out);
							}
							else if(order.equals("waituserlist"))
							{
								waitUserList(out);
							}
							else if(order.equals("myroomlist"))
							{
								title = chatUserMap.get(name);
								System.out.println(name);
								myRoomList(name, title, out);
							}
							else if(order.equals("invite"))
							{
								out.println("초대할사람입력: ");
								String man = in.readLine();
								PrintWriter to_out = (PrintWriter)clientMap.get(man);
								BufferedReader to_in = (BufferedReader)inviteInfoMap.get(man);
								out = to_out;
								out.println(name+" 님이 초대하셨습니다. 입장하시겠습니까?  y? or n?");
								System.out.println("-------1");
								
								String answer= in.readLine();															
								System.out.println("-------2");
								
								inviteCheck(name, man, answer);
							}
							else if(order.equals("kick"))
							{
								title = chatUserMap.get(name);
								out.println("강퇴할 사람 입력:");
								String kick = in.readLine();
								PrintWriter to_out = (PrintWriter)clientMap.get(kick);
								kickCheck(name, title, out, kick, to_out);
							}
							else if(order.equals("ban"))
							{
								title = chatUserMap.get(name);
								out.println("영구강퇴할 사람 입력:");
								String ban = in.readLine();
								PrintWriter to_out = (PrintWriter)clientMap.get(ban);
								banCheck(name, title, out, ban, to_out);
							}
							else if(order.equals("exit"))
							{
								System.out.println("ㅡㅡㅡㅡㅡ19");
								title = chatUserMap.get(name);
								if(openInfoMap.containsKey(title))
								{
									openRoomExit(name, title, out);								
								}
								else if(secretInfoMap.containsKey(title))
								{
									secretRoomExit(name,title, out);
								}
								else
								{
									out.println("잘못된 입력");
								}
//								exitRoom(name, title, out);
							}
							else if(order.equals("succession"))
							{
								title = chatUserMap.get(name);
								out.println("승계할 대상입력: ");
								String man = in.readLine();
								successionOwner(name, title, man);
							}
							else if(order.equals("close"))
							{
								System.out.println("ㅡㅡㅡㅡㅡ14");
								title = chatUserMap.get(name);
								if(openInfoMap.containsKey(title))
								{
									closepublicRoom(name, title, out);
								}
								else
								{
									closeSecretRoom(name, title, out);
								}
								
							}
									
							else
							{
								System.out.println("???");
							}
						}else {
							title = chatUserMap.get(name);
							sendChatRoom(name, title, s);
						}
					}
					else
					{
						if ( s.substring(0, 1).equals("/"))
						{
							StringTokenizer str = new StringTokenizer(s.substring(1));
							String order = str.nextToken();
							System.out.println("ㅡㅡㅡㅡb");
							
							Object obj = clientMap.get(name);
							PrintWriter out = (PrintWriter)obj;
						//	out.println("/menu 로 명령어메뉴확인하세요");
							if(order.equals("to"))	
							{	sendTo(name, s);	}
							else if(order.equals("list"))
							{	list(out);		}
							else if(order.equals("menu"))
							{	waitingRoomMenu(name);}
							else if(order.equals("makeroom"))
							{
								System.out.println("ㅡㅡㅡㅡc");
								out.println("타이틀(방제목)설정 : ");
								title = in.readLine();
								out.println("정원 설정 : ");
								int number = Integer.parseInt(in.readLine());
								makeRoom(name,title,number,out);
							}
							else if(order.equals("secretroom"))
							{
								System.out.println("ㅡㅡㅡㅡc");
								out.println("타이틀(방제목)설정 : ");
								title = in.readLine();
								out.println("정원 설정 : ");
								int number = Integer.parseInt(in.readLine());
								out.println("비번 설정 : ");
								int pw = Integer.parseInt(in.readLine());
								secretroom(name,title,number,pw,out);
							}
							else if(order.equals("roomlist"))
							{
								roomlist(out);
							}
							else if(order.equals("openlist"))
							{
								openlist(out);
							}
							else if(order.equals("secretlist"))
							{
								secretlist(out);
							}
							else if (order.equals("enter"))
							{
								System.out.println("ㅡㅡㅡㅡd");
								out.println("방제목입력: ");
								title = in.readLine();
								System.out.println("ㅡㅡㅡㅡㅂ");
								if(title.equals(banListMap.get(name)))
								{
									out.println("영구강퇴 당해서 입장불가");
								}
								else
								{
									//먼저 비공개방인지 아닌지 검사여부
									if(secretInfoMap.containsKey(title)) 
									{
										System.out.println("ㅡㅡㅡㅡㅈ");
										out.println("비번 입력 : ");
										int pw = Integer.parseInt(in.readLine());
										System.out.println("ㅡㅡㅡㅡdㄷ");
										if(pw == pwInfoMap.get(title))
										{
											enterSecretRoom(name, title, out);									
										}else {
											out.println("비번 틀림, Bye");
										}
									}else {
										enterRoom(name, title, out);	
									}				
								}
							}
							else {
								sendAllMsg(name, s);
								out.println("잘못된명령어ㅡㅡ");
							}
						}else {
							System.out.println("ㅡㅡㅡㅡ2");
							if(chatUserMap.containsKey(name))
							{
								System.out.println("ㅡㅡㅡㅡ3");
								title = chatUserMap.get(name);							
								sendChatRoom(name, title, s);
							}else {
								System.out.println("ㅡㅡㅡㅡ4");
								guestMsg(name, s);
								//sendAllMsg(name, s);	
								System.out.println(" ");
							}
						
						}
					
					}
				}	

				
//				System.out.println("Bye...");
				
			} catch(Exception e) {
				System.out.println("예외:" +e);
			} finally {
				//예외가 발생할 때 퇴장. 해쉬맵에서 해당 데이터 제거.
				//보통 종료하거나 나가면 java.net.SocketException:예외발생
				
				clientMap.remove(name);
				sendAllMsg("", name + "님이 대기실에서 퇴장하셨습니다.");
				System.out.println("현재 접속자 수는 "+clientMap.size()+"명 입니다.");
				
				try {
					in.close();
					out.close();
					
					socket.close();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	//////////////////////////////////////////////////////////////
}
