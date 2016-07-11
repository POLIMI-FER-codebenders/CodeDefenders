<!DOCTYPE html>
<html>

<head>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

	<!-- Title -->
	<title>Code Defenders - My Games</title>

	<!-- App context -->
	<base href="${pageContext.request.contextPath}/">

	<!-- jQuery -->
	<script src="js/jquery.min.js" type="text/javascript" ></script>

	<!-- Bootstrap -->
	<script src="js/bootstrap.min.js" type="text/javascript" ></script>
	<link href="css/bootstrap.min.css" rel="stylesheet" type="text/css" />

	<!-- Game -->
	<link href="css/gamestyle.css" rel="stylesheet" type="text/css" />

</head>

<body>

<%@ page import="org.codedefenders.DatabaseAccess" %>
<%@ page import="org.codedefenders.Game" %>
<%@ page import="java.util.ArrayList" %>
<nav class="navbar navbar-inverse navbar-fixed-top">
	<div class="container-fluid">
		<div class="navbar-header">
			<button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar-collapse-1" aria-expanded="false">
			</button>
			<a class="navbar-brand" href="/">
				<span><img class="logo" href="/" src="images/logo.png"/></span>
				Code Defenders
			</a>
		</div>
		<div class= "collapse navbar-collapse" id="navbar-collapse-1">
			<ul class="nav navbar-nav">
				<li class="active"><a href="games/user">My Games</a></li>
				<li><a href="games/open">Open Games</a></li>
				<li><a href="games/create">Create Game</a></li>
				<li><a href="games/history">History</a></li>
			</ul>
			<ul class="nav navbar-nav navbar-right">
				<li></li>
				<li>
					<p class="navbar-text">
						<span class="glyphicon glyphicon-user" aria-hidden="true"></span>
						<%=request.getSession().getAttribute("username")%>
					</p>
				</li>
				<li><input type="submit" form="logout" class="btn btn-inverse navbar-btn" value="Log Out"/></li>
			</ul>
		</div>
	</div>
</nav>

<form id="logout" action="login" method="post">
	<input type="hidden" name="formType" value="logOut">
</form>

<h2> My Games </h2>
<table class="table table-hover table-responsive table-paragraphs">
	<tr>
		<td class="col-sm-2">Game No.</td>
		<td class="col-sm-2">Attacker</td>
		<td class="col-sm-2">Defender</td>
		<td class="col-sm-2">Game State</td>
		<td class="col-sm-2">Class Under Test</td>
		<td class="col-sm-2">Level</td>
		<td class="col-sm-2"></td>
	</tr>
<%
	String atkName;
	String defName;
	int uid = (Integer)request.getSession().getAttribute("uid");
	ArrayList<Game> games = DatabaseAccess.getGamesForUser(uid);
	if (games.isEmpty()) {
%>
	<p> You are not currently in any games. </p>
<%
	} else {
		for (Game g : games) {
			atkName = null;
			defName = null;

			if (g.getAttackerId() != 0) {
				atkName = DatabaseAccess.getUserForKey("User_ID", g.getAttackerId()).username;
			}

			if (g.getDefenderId() != 0) {
				defName = DatabaseAccess.getUserForKey("User_ID", g.getDefenderId()).username;
			}

			int turnId = g.getAttackerId();
			if (g.getActiveRole().equals(Game.Role.DEFENDER))
				turnId = g.getDefenderId();

			if (atkName == null) {atkName = "Empty";}
			if (defName == null) {defName = "Empty";}
%>
	<tr>
		<td class="col-sm-2"><%= g.getId() %></td>
		<td class="col-sm-2"><%= atkName %></td>
		<td class="col-sm-2"><%= defName %></td>
		<td class="col-sm-2"><%= g.getState() %></td>
		<td class="col-sm-2"><%= g.getCUT().getAlias() %></td>
		<td class="col-sm-2"><%= g.getLevel().name() %></td>
		<td class="col-sm-2">
<%
			if (g.getState().equals(Game.State.ACTIVE)) { // Can enter only if game is in progress.
				String btnLabel = "Your Turn";
				if (g.getMode().equals(Game.Mode.UTESTING)) {
					btnLabel = "Enter";
				}
%>
			<form id="view" action="games" method="post">
				<input type="hidden" name="formType" value="enterGame">
				<input type="hidden" name="game" value="<%=g.getId()%>">
				<% if (uid == turnId ) {%>
				<input class="btn btn-primary" type="submit" value="<%=btnLabel%>">
				<% } else {%>
				<input  class="btn btn-default" type="submit" value="Enter Game">
				<% }%>
			</form>

<%
			}
%>
		</td>
	</tr>
<%
		} // for (Game g : games)
	} // if (games.isEmpty())
%>
</table>

</body>
</html>
