<%--

    Copyright (C) 2016-2019 Code Defenders contributors

    This file is part of Code Defenders.

    Code Defenders is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Code Defenders is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ page pageEncoding="UTF-8" %>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>

<!DOCTYPE html>
<html>

<head>
    <title>${pageInfo.pageTitle}</title>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

    <!-- App context -->
    <base href="${pageContext.request.contextPath}/">

    <link href="css/global/variables.css" rel="stylesheet">

    <!-- Favicon.ico -->
    <link rel="icon" href="favicon.ico" type="image/x-icon">

    <!-- JS Init -->
    <script type="module">
        import './js/codedefenders_init.mjs';
    </script>

    <!-- Bootstrap -->
    <link href="webjars/bootstrap/5.0.1/css/bootstrap.min.css" rel="stylesheet">
    <link href="css/global/bootstrap_customize.css" rel="stylesheet">

    <!-- Codemirror -->
    <link href="webjars/codemirror/5.22.0/lib/codemirror.css" rel="stylesheet">
    <link href="webjars/codemirror/5.22.0/addon/dialog/dialog.css" rel="stylesheet">
    <link href="webjars/codemirror/5.22.0/addon/search/matchesonscrollbar.css" rel="stylesheet">
    <link href="webjars/codemirror/5.22.0/addon/hint/show-hint.css" rel="stylesheet">
    <link href="css/global/codemirror_customize.css" rel="stylesheet">

    <link href="webjars/font-awesome/4.7.0/css/font-awesome.min.css" rel="stylesheet">

    <!-- DataTables -->
    <link href="webjars/datatables/1.10.24/css/dataTables.bootstrap5.min.css" rel="stylesheet">
    <%-- <link href="webjars/datatables-select/1.3.3/css/select.bootstrap5.min.css" rel="stylesheet"> We use custom CSS instead. --%>
    <link href="css/global/datatables_customize.css" rel="stylesheet">

    <link href="css/global/page.css" rel="stylesheet">
    <link href="css/global/common.css" rel="stylesheet">
    <link href="css/global/loading_animation.css" rel="stylesheet">
</head>

<body>
