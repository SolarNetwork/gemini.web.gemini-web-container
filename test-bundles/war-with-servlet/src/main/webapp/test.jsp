<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="java.util.*, java.net.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
</head>
<body>
<%
  int i = 0;
  Enumeration<URL> e = this.getClass().getClassLoader().getResources("test.txt");
  while (e.hasMoreElements()) {
    out.println(e.nextElement());
    i++;
  }
  out.println("Found resources " + i);

  int j = 0;
  if (config.getServletContext().getResourceAsStream("/test.jsp") != null) {
      j++;
  }
  if (config.getServletContext().getResourceAsStream("/WEB-INF/lib/resource.jar") != null) {
      j++;
  }
  if (config.getServletContext().getResourceAsStream("WEB-INF/lib/resource.jar") != null) {
      j++;
  }
  out.println("Found input sources " + j);

  int k = 0;
  Set<String> resourcePaths = config.getServletContext().getResourcePaths("/WEB-INF/lib");
  for (String resource : resourcePaths) {
      if (config.getServletContext().getResourceAsStream(resource) != null) {
          k++;
      }
  }
  out.println("Found resource paths " + k);
%>

</body>
</html>