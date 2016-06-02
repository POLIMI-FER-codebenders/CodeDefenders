<!DOCTYPE html>
<html>

<head>
    <title>Code Defenders - Attack</title>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

    <!-- App context -->
    <base href="${pageContext.request.contextPath}/">

    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <!-- Slick -->
    <link rel="stylesheet" type="text/css" href="//cdn.jsdelivr.net/jquery.slick/1.5.9/slick.css"/>
    <script type="text/javascript" src="//cdn.jsdelivr.net/jquery.slick/1.5.9/slick.min.js"></script>

    <!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="js/bootstrap.min.js"></script>


    <!-- Bootstrap -->
    <link href="css/bootstrap.min.css" rel="stylesheet">
    <link href="css/gamestyle.css" rel="stylesheet">

    <!-- Leaf -->
    <link href="css/base.css" rel="stylesheet">
    <script type="text/javascript" src="js/script.js"></script>

    <script src="codemirror/lib/codemirror.js"></script>
    <script src="codemirror/mode/javascript/javascript.js"></script>
    <script src="codemirror/mode/diff/diff.js"></script>
    <link href="codemirror/lib/codemirror.css" rel="stylesheet">


    <script>
        $(document).ready(function() {
            $('.single-item').slick({
                arrows: true,
                infinite: true,
                speed: 300,
                draggable:false
            });
            $('#messages-div').delay(10000).fadeOut();
        });
    </script>
</head>

<body class="page-grid">
<%@ page import="java.util.*" %>
<%@ page import="org.codedefenders.Test" %>
<%@ page import="org.codedefenders.Mutant" %>
<%@ page import="org.codedefenders.Game" %>
<%@ page import="org.codedefenders.Constants" %>
<%@ page import="org.codedefenders.DatabaseAccess" %>
<%@ page import="org.codedefenders.Game" %>
<%@ page import="static org.codedefenders.Game.State.ACTIVE" %>
<%@ page import="org.codedefenders.GameClass" %>
    <% Game game = (Game) session.getAttribute("game"); %>
<div class="top bg-grey bg-plus-4 text-white" style="padding-bottom:0px;">
    <div class="full-width">
        <div class="nest">
            <div class="crow fly nogutter">
                <div>
                    <div>
                        <div class="tabs-blue-grey">
                            <a href="/" class="main-title">
                                <div class="crow">
                                    <div class="w-6">
                                        <span><img class="logo" href="/" src="images/logo.png"/></span>
                                    </div>
                                    <div class="down w-94" style="font-size: 36px;">
                                        &gt;code defenders
                                    </div>
                                </div>
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    <div class="bg-plus-3" style="padding:2px 0; margin-bottom: 0px; margin-top: 5px;"></div>
    </div>
</div>
<div class="top bg-blue-grey text-white" style="height: 50px; padding: 10px;">
    <div class="full-width" style="padding-top: 3px;">
        <div class="nest">
            <div class="crow">
                <div class="ws-9" style="text-align: left">
                    <ul class="inline">
                        <li><div class="button tab-link text-white relative bg-minus-1">Manage Games
                            <div class="bg-white relative drop down-left card baseline-padding crow no-gutter ws-12">
                                <ul class="unstyled">
                                    <li>
                                        <a class="text-white list-item " href="games/user">My Games</a>
                                    </li>
                                    <li>
                                        <a class="text-white list-item" href="games/open">Open Games</a>
                                    </li>
                                    <li>
                                        <a class="text-white list-item" href="games/create">Create Game</a>
                                    </li>
                                    <li>
                                        <a class="text-white list-item" href="games/history">History</a>
                                    </li>
                                </ul>
                            </div>
                        </div>
                        </li>
                    </ul>
                </div>
                <div class="ws-3" style="text-align: right;">
                    <div class="tabs-blue-grey ws-12">
                        <ul class="inline">
                            <li>
                                <div class="button tab-link text-white relative bg-minus-1 ws-12">
                                    <p style="text-align: left; display: inline; width:70%">
                                        <%=request.getSession().getAttribute("username")%>
                                    </p>
                                    <span style="text-align: right" class="glyphicon glyphicon-user" aria-hidden="true"></span>
                                    <div class="bg-white relative drop down-left card baseline-padding ws-12">
                                        <ul class="unstyled">
                                            <li>
                                                <a href="#" class="text-white list-item" onclick="$('.logout').submit();">
                                                    <span class="glyphicon glyphicon-remove"></span>
                                                    logout
                                                </a>
                                            </li>
                                        </ul>
                                    </div>
                                </div>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<form id="logout" action="login" method="post">
    <input type="hidden" name="formType" value="logOut">
</form>

    <%
	ArrayList<String> messages = (ArrayList<String>) request.getSession().getAttribute("messages");
	request.getSession().removeAttribute("messages");
	if (messages != null && ! messages.isEmpty()) {
%>
<div class="alert alert-info" id="messages-div">
    <% for (String m : messages) { %>
    <pre><strong><%=m%></strong></pre>
    <% } %>
</div>
<%	} %>
<div class="nest">
    <div class="full-width">
        <div class="bg-plus-2" style="padding:2px 0;">
        </div>
        <h2 class="full-width page-title"><%= pageTitle %></h2>
        <div class="nest">
            <div class="crow fly">
                <div class="crow fly">