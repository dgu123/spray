<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<title>
		<spring:message 
			code="DatabaseMessageSourceIntegrationTest.title" text="For testing DatabaseMessageSource" />
		
	</title>
</head>
<body>
	<h1 id="h1_tag">
		<spring:message code="DatabaseMessageSourceIntegrationTest.h1" text="Default header" />
	</h1><br />
	<p id="p_1_tag">Current time: ${date}</p>
	<p id="p_2_tag">Old Locale : ${oldLocaleName}</p>
	<p id="p_3_tag">Locale: ${pageContext.response.locale}</p>
	<p id="p_4_tag">Locale Resovler class name: ${localeResolverClassName}</p>
</body>
</html>