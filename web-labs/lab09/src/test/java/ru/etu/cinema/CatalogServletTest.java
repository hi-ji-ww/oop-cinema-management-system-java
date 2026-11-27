package ru.etu.cinema;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.io.*;
import javax.servlet.ServletConfig;
import javax.servlet.http.*;
import org.junit.*;
/** Проверки сервлета без запуска Tomcat с использованием Mockito. */
public class CatalogServletTest {
 private CatalogServlet servlet;
 private HttpServletRequest request;
 private HttpServletResponse response;
 private StringWriter body;
 @Before public void setUp() throws Exception {
  request=mock(HttpServletRequest.class);response=mock(HttpServletResponse.class);
  ServletConfig config=mock(ServletConfig.class);
  when(config.getInitParameter("cinemaName")).thenReturn("Кинотеатр Север");
  body=new StringWriter();when(response.getWriter()).thenReturn(new PrintWriter(body));
  servlet=new CatalogServlet();servlet.init(config);
 }
 @Test public void noFilterReturnsThreeFilms() throws Exception {
  servlet.doGet(request,response);
  assertTrue(body.toString().contains("Найдено сеансов: 3"));
  verify(request).setCharacterEncoding("UTF-8");verify(response).setContentType("text/html;charset=UTF-8");
 }
 @Test public void dramaReturnsTwoFilms() throws Exception {
  when(request.getParameter("genre")).thenReturn("drama");servlet.doGet(request,response);
  assertTrue(body.toString().contains("Космический рейс"));
  assertTrue(body.toString().contains("Дорога домой"));
  assertFalse(body.toString().contains("Огни города"));
  assertTrue(body.toString().contains("Найдено сеансов: 2"));
 }
 @Test public void comedyReturnsOneFilm() throws Exception {
  when(request.getParameter("genre")).thenReturn("comedy");servlet.doGet(request,response);
  assertTrue(body.toString().contains("Огни города"));assertTrue(body.toString().contains("Найдено сеансов: 1"));
 }
 @Test public void invalidGenreReturnsBadRequest() throws Exception {
  when(request.getParameter("genre")).thenReturn("<script>alert(1)</script>");servlet.doGet(request,response);
  verify(response).sendError(400,"Неизвестный жанр");verify(response,never()).getWriter();
 }
 @Test public void initParameterIsEscaped() throws Exception {
  ServletConfig config=mock(ServletConfig.class);when(config.getInitParameter("cinemaName")).thenReturn("<b>Север</b>");
  servlet.init(config);servlet.doGet(request,response);
  assertTrue(body.toString().contains("&lt;b&gt;Север&lt;/b&gt;"));assertFalse(body.toString().contains("<b>Север</b>"));
 }
 @Test public void missingInitParameterUsesDefault() throws Exception {
  servlet.init(mock(ServletConfig.class));servlet.doGet(request,response);
  assertTrue(body.toString().contains("Кинотеатр Север"));
 }
}
