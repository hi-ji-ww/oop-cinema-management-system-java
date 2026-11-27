<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.net.URLEncoder,java.time.Instant,ru.etu.cinema.Html" %>
<% request.setCharacterEncoding("UTF-8");
 // Обновление выполняется только после отправки формы.
 if("POST".equals(request.getMethod())) {
  String name=request.getParameter("name"), color=request.getParameter("color");
  if(name==null||name.trim().isEmpty()||name.length()>80) { request.setAttribute("error","Имя должно содержать от 1 до 80 символов"); }
  else if(color==null||!color.matches("#[0-9A-Fa-f]{6}")) { request.setAttribute("error","Неверный цвет"); }
  else {
   // В Cookie нет учетных данных. Имя кодируется в UTF-8 и URL encoding.
   Cookie a=new Cookie("cinemaName",URLEncoder.encode(name.trim(),"UTF-8"));
   Cookie b=new Cookie("cinemaColor",color.substring(1));
   for(Cookie c:new Cookie[]{a,b}) { c.setPath(request.getContextPath()); c.setMaxAge(604800); c.setHttpOnly(true); c.setSecure(request.isSecure()); response.addCookie(c); }
   synchronized(session) {
    Integer visits=(Integer)session.getAttribute("visits");
    session.setAttribute("visits",visits==null?1:visits+1);
    session.setAttribute("previousVisit",session.getAttribute("lastVisit"));
    session.setAttribute("lastVisit",Instant.now().toString());
   }
   // Перенаправление создает следующий запрос, в нем Cookie уже доступны.
   response.sendRedirect(request.getContextPath()+"/result.jsp"); return;
  }
 } %>
<!doctype html><html lang="ru"><meta charset="UTF-8"><title>Настройки</title><link rel="stylesheet" href="style.css"><h1>Настройки посетителя</h1>
<form method="post"><label>Имя <input name="name" maxlength="80" required></label><label>Цвет страницы <input name="color" type="color" value="#dceeff"></label><button>Сохранить</button></form>
<p class="error"><%= Html.escape((String)request.getAttribute("error")) %></p><a href="result.jsp">Посмотреть сохранённые настройки</a></html>
