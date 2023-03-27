package edu.kh.community.board.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.oreilly.servlet.MultipartRequest;

import edu.kh.community.board.model.service.BoardService;
import edu.kh.community.board.model.vo.BoardDetail;
import edu.kh.community.board.model.vo.BoardImage;
import edu.kh.community.common.MyRenamePolicy;
import edu.kh.community.member.model.vo.Member;

// 컨트롤러 : 요청에 따라 알맞은 Service를 호출하고 결과에 따라 응답을 제어
@WebServlet("/board/write")
public class BoardWriteController extends HttpServlet{

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	
		try {
			String mode = req.getParameter("mode"); // insert / update 구분
			
			// insert는 별도 처리 없이 jsp로 위임
			
			// update는 기존 게시글 내용을 조회하는 처리가 필요함
			if(mode.equals("update")) {
				
				int boardNo =  Integer.parseInt( req.getParameter("no") );
				
				// 게시글 상세조회 서비스를 이용해서 기존 내용 조회
				// ( new BoardService() : 객체를 생성해서 변수에 저장 X -> 1회성 사용 )
				BoardDetail detail = new BoardService().selectBoardDetail(boardNo);
				
				// 개행문자 처리 해제( <br> -> \n )
				detail.setBoardContent(  detail.getBoardContent().replaceAll("<br>", "\n") );
				
				req.setAttribute("detail", detail); // jsp에서 사용할 수 있도록 req에 값 세팅
				
			}
			

			// 동적 웹 : 클라이언트 요청(주소)에 따라 응답화면을 만들어서 보여줌
			
			// -> Servlet : 요청에 따라 응답 화면(Java에서 HTML 코드 작성)을 만들어서 클라이언트 전달
			//  -> PrintWriter out = resp.getWriter(); // 클라이언트와 연결된 응답용 스트림
			//    out.print("<html>");
			//    out.print("<head>"); ..
			//  -> 너무 힘들어... 불편해 ... 
			//  -> JSP(Java Server Page) : 표기법은 HTML이지만 실제로는 Java코드(Servlet으로 변환됨)
			
			
			// [forward] : 요청 위임
			
			// Servlet은 요청을 받아서 백엔드(Service, DB) 처리
			// -> 처리 결과를 JSP에게 위임해서 응답 화면(HTML)로 만들게 함
			//	--> Servlet의 req, resp를 그대로 JSP에게 넘겨줘야 함
			
			// 특징 1) req, resp가 유지됨 
			// -> 파라미터, req.setAttribute()로 추가된 값을 JSP에서 그대로 사용 가능
			
			// 특징 2) Servlet -> JSP 두개의 페이지로 보이지만 실제로는 하나
			//		  (둘이 하나의 일을 협업한다고 생각하자)
			//		  -> Servlet에서 JSP 요청 위임을 해도 요청주소는 변하지 않는다.
			
			
			// [redirect] : 재요청
			// 요청 처리 후 응답 화면을 새롭게 만드는 것이 아닌
			// 응답 화면을 만들어 주는(forward) 요청 주소를 클라이언트에게 전달
			// -> 클라이언트가 해당 주소로 다시 요청(재요청)
			
			// 클라이언트가 원하는 결과를 보여줄 수 있는 주소를 알려주어 이동하게 함
			
			// (클)국밥 주세요 -> (서)카페 
			//					   --> 길건너 국밥집으로 가세요(redirect)
			
			// (클)국밥 주세요 -> (서)국밥
			//	--> 다시 요청
			
			// 특징 1) req, resp 객체가 새롭게 생성됨
			//		-> 기존 req,resp가 유지되지 않기 때문에
			//		   request scope에 세팅된 값(파라미터, req.setAttribute())가 유지되지 않음.
			//			--> 대안으로 Session 활용
			
			// 특징 2) 다시 요청을 하기 때문에 주소가 변함.
			
			String path ="/WEB-INF/views/board/boardWriteForm.jsp";
			req.getRequestDispatcher(path).forward(req, resp);
			
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	
	
	
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {
			// insert/update 구분 없이 전달 받은 파라미터 모두 꺼내서 정리하기
			
			// *** enctype="multipart/form-data" 인코딩 미지정 형식의 요청 ***
			// -> HttpServletRequest로 파라미터 얻어오기 불가능!
			// --> MultipartRequest를 이용(cos.jar 라이브러리 제공)
			// ---> 업로드 최대 용량, 저장 실제 경로, 파일명 변경 정책, 문자 파라미터 인코딩 설정 필요
			
			int maxSize = 1024 * 1024 * 100; // 업로드 최대 용량 (100MB)
			
			HttpSession session = req.getSession(); // session 얻어오는 것은 지장없음(사용 가능)
			String root = session.getServletContext().getRealPath("/"); // webapp 폴더까지 경로
			String folderPath = "/resources/images/board/"; // 파일 저장 폴더 경로
			String filePath = root + folderPath;
			
			String encoding = "UTF-8"; // 파라미터 중 파일 제외 파라미터(문자열)의 인코딩 지정
			
			
			// *** MultipartRequest 객체 생성! ***
			// -> 객체가 생성됨과 동시에 파라미터로 전달된 파일이 지정된 경로에 저장(업로드)된다!
			MultipartRequest mpReq = new MultipartRequest(req, filePath, maxSize, encoding, new MyRenamePolicy());                
			
			
			// MultipartRequest.getFileNames()
			// - 요청 파라미터 중 모든 file 타입 태그의 name속성 값을 얻어와
			//   Enumeration 형태로 반환  (Iterator의 과거 버전)
			//   --> 해당 객체에 여러 값이 담겨있고 이를 순서대로 얻어오는 방법을 제공
			//      (보통 순서가 없는 모음(Set과 같은 경우)에서 하나씩 꺼낼 때 사용)
			
			Enumeration<String> files = mpReq.getFileNames(); 
			// file 타입 태그의 name 속성 0,1,2,3,4가 순서가 섞인 상태로 얻어와짐
			
			
			// * 업로드된 이미지의 정보를 모아둘 List 생성
			List<BoardImage> imageList = new ArrayList<BoardImage>();
			
			
			while(files.hasMoreElements()) { // 다음 요소가 있으면 true
				String name = files.nextElement(); // 다음 요소(name 속성 값)를 얻어옴
				
				//System.out.println("name : " + name);
				// file 타입 태그의 name 속성 값이 얻어와짐
				// + 업로드가 안된 file 타입 태그의 name도 얻어와짐
				
				String rename = mpReq.getFilesystemName(name);   // 변경된 파일명
				String original = mpReq.getOriginalFileName(name); // 원본 파일명
				
				//System.out.println("rename : " + rename);
				//System.out.println("original : " + original);
				
				
				if(rename != null) { // 업로드된 파일이 있을 경우 == 
								   // 현재 files에서 얻어온 name속성을 이용해 
								   // 원본 또는 변경을 얻어왔을 때 그 값이 null이 아닌경우
					
					// 이미지 정보를 담은 객체(BoardImage)를 생성
					BoardImage image = new BoardImage();
					
					image.setImageOriginal(original); // 원본명 (다운로드 시 사용)
					image.setImageReName(folderPath + rename); // 폴더 경로 + 변경명
					image.setImageLevel( Integer.parseInt(name) ); // 이미지 위치(0은 썸네일)
				
					imageList.add(image); // 리스트에 추가
				
				} // if 끝
				
			} // while 끝
			
			
			
			// * 이미지를 제외한 게시글 관련 정보 *
			String boardTitle = mpReq.getParameter("boardTitle");
			String boardContent = mpReq.getParameter("boardContent");
			int boardCode = Integer.parseInt( mpReq.getParameter("type") ); // hidden
			
			
			Member loginMember = (Member)session.getAttribute("loginMember"); // 로그인 정보 얻어오기
			int memberNo = loginMember.getMemberNo(); // 회원 번호
			
			
			// 게시글 관련 정보를 하나의 객체(BoardDetail)에 담기
			BoardDetail detail = new BoardDetail();
			
			detail.setBoardTitle(boardTitle);
			detail.setBoardContent(boardContent);
			detail.setMemberNo(memberNo);
			// boardCode는 별도 매개변수로 전달 예정
			
			// ------------- 게시글 작성에 필요한 기본 파라미터 얻어오기 끝 -------------
			
			BoardService service = new BoardService();
			
			// 모드 (insert/update)에 따라서 추가 파라미터 얻어오기 및 서비스 호출
			String mode = mpReq.getParameter("mode") ; // hidden
			
			if(mode.equals("insert")) { // 삽입
				
				// 게시글 삽입 서비스 호출 후 결과(삽입된 게시글 번호) 반환 받기
				// -> 반환된 게시글 번호를 이용해서 상세조회로 리다이렉트 예정
				int boardNo = service.insertBoard(detail, imageList, boardCode);
				
				
				String path = null;
				if(boardNo > 0) { // 성공
					session.setAttribute("message", "게시글이 등록되었습니다.");
					// detail?no=2000&type=2
					path = "detail?no=" + boardNo + "&type=" + boardCode;
					
				}else { // 실패
					session.setAttribute("message", "게시글 등록 실패");
					
					// write?mode=insert&type=2
					path = "write?mode=" + mode + "&type=" + boardCode;
				}
				
				resp.sendRedirect(path); // 리다이렉트
				
			}
			
			
			
			if(mode.equals("update")) { // 수정
				// 앞선 코드는 동일(업로드된 이미지 저장, imageList 생성, 제목/내용 파라미터 동일)
				
				// + update일 때 추가된 내용
				// 어떤 게시글 수정? -> 파라미터 no
				// 나중에 목록으로 버튼 만들 때 사용할 현재페이지 -> 파라미터 cp
				// 이미지 중 x버튼을 눌러서 삭제할 이미지 레벨 목록 -> 파라미터 deleteList
				int boardNo = Integer.parseInt( mpReq.getParameter("no") );
				
				int cp = Integer.parseInt( mpReq.getParameter("cp") );
				
				String deleteList = mpReq.getParameter("deleteList"); // 1,3,4
				
				
				// 게시글 수정 서비스 호출 후 결과 반환 받기
				detail.setBoardNo(boardNo);
				
				// detail, imageList, deleteList
				int result = service.updateBoard(detail, imageList, deleteList);
				
				String path = null;
				String message = null;
				
				if(result > 0) { // 성공
					
					// detail?no=1000&type=1&cp=20
					path = "detail?no=" + boardNo + "&type=" + boardCode + "&cp=" + cp; // 상세조회 페이지 요청 주소
					
					message = "게시글이 수정되었습니다.";
					
				}else { // 실패
					
					// 수정화면으로 이동
					
					// 상세조회 -> 수정화면 -> 수정 -> (성공)상세조회
					//								-> (실패)수정화면
					
					// write?mode=update&type=2&cp=1&no=1525
					path = req.getHeader("referer");
					// referer : HTTP 요청 흔적(요청 바로 이전 페이지 주소)
					
					message = "게시글 수정 실패";
				}
				
				session.setAttribute("message", message);
				
				resp.sendRedirect(path);
			}
			
			
		}catch (Exception e) {
			e.printStackTrace();
		}
		
	
	}
	
	
	
	
	
	
	
	
	
	
}
