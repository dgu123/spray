<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title>For testing ViewResolverConfig</title>
</head>
<body>
	<h1><spring:message code="ViewResolverConfigTest.title" text="For testing ViewResolverConfig" /></h1>
	<br />
	Current time: ${date}<br />
	<br />
	Old Locale : ${oldLocaleName}<br />
	<br />
	Locale: ${pageContext.response.locale}<br />
	<br />
	Locale Resovler class name: ${localeResolverClassName}
</body>
</html>