package edu.kh.community.member.controller;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.kh.community.member.model.service.MemberService;
import edu.kh.community.member.model.vo.Member;

@WebServlet("/member/signUp")
public class SignUpServlet extends HttpServlet{

	// GET 방식 요청 시 JSP로 바로 응답할 수 있도록 요청 위임
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	
		String path = "/WEB-INF/views/member/signUp.jsp";
		req.getRequestDispatcher(path).forward(req, resp);
	}
	
	
	// POST 방식 요청 시 회원가입 서비스 수행
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	
		// 파라미터를 모두 변수에 저장
		String memberEmail = req.getParameter("memberEmail");
		String memberPw = req.getParameter("memberPw");
		String memberNickname = req.getParameter("memberNickname");
		String memberTel = req.getParameter("memberTel");
		
		// 주소는 3개의 input으로 이루어져 있으므로 배열로 전달 받음
		// -> DB 컬럼을 1개이므로 배열을 하나의 문자열로 합칠 예정
		String[] address = req.getParameterValues("memberAddress");
		
		// 주소가 입력되지 않으면 null이 아니라 빈칸 3개
		
		String memberAddress = null;
		
		if(!address[0].equals("")) { // 우편번호가 빈칸이 아니라면 == 주소 작성
			memberAddress = String.join(",,", address);
			
			// String.join("구분자", 배열)
			// -> 배열 요소를 하나의 문자열로 반환
			//    요소 사이에 "구분자" 추가
		}
		
		
		// 파라미터를 하나의 Member 객체에 저장
		Member mem = new Member();
		
		mem.setMemberEmail(memberEmail);
		mem.setMemberPw(memberPw);
		mem.setMemberNickname(memberNickname);
		mem.setMemberTel(memberTel);
		mem.setMemberAddress(memberAddress);
		
		
		try {
			
			MemberService service = new MemberService();
			
			// 회원가입 서비스 호출 후 결과 반환 받기
			int result = service.signUp(mem);
			
			// 서비스 결과에 따라서 message를 다르게하여 메인 페이지 재요청(redirect)
			
			HttpSession session = req.getSession();
			
			if(result > 0) { // 성공
				session.setAttribute("message", "회원 가입 성공!!");
				
			}else { // 실패
				session.setAttribute("message", "회원 가입 실패...");
				
			}
			
			resp.sendRedirect( req.getContextPath() );
			
		}catch (Exception e) {
			e.printStackTrace();
		}
		
	
	}
	
	
	
	
	
}
