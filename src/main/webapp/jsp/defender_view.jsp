<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.game.GameState" %>

<% String pageTitle="Defending Class"; %>
<%@ include file="/jsp/header_game.jsp" %>

<% { %>

<%-- TODO Set request attributes in the Servlet and redirect via RequestDispatcher? --%>

<%-- Set request attributes for the components. --%>
<%
	/* class_viewer */
	request.setAttribute("classCode", game.getCUT().getAsString());
	request.setAttribute("mockingEnabled", game.getCUT().isMockingEnabled());

	/* test_editor */
	String previousTestCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
	request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
	if (previousTestCode != null) {
		request.setAttribute("testCode", previousTestCode);
	} else {
		request.setAttribute("testCode", game.getCUT().getTestTemplate());
	}

	/* tests_carousel */
	request.setAttribute("tests", game.getTests());

	/* mutants_list */
    request.setAttribute("mutantsAlive", game.getAliveMutants());
    request.setAttribute("mutantsKilled", game.getKilledMutants());
    request.setAttribute("mutantsEquivalent", game.getMutantsMarkedEquivalent());
	request.setAttribute("markEquivalent", true);
    request.setAttribute("markUncoveredEquivalent", false);
    request.setAttribute("viewDiff", game.getLevel() == GameLevel.EASY);
	request.setAttribute("gameType", "DUEL");

	/* game_highlighting */
	request.setAttribute("codeDivSelector", "#cut-div");
	// request.setAttribute("tests", game.getTests());
	request.setAttribute("mutants", game.getMutants());
	request.setAttribute("showEquivalenceButton", true);

	/* finished_modal */
    int attackerScore = game.getAttackerScore();
    int defenderScore = game.getDefenderScore();
	request.setAttribute("win", defenderScore > attackerScore);
	request.setAttribute("loss", attackerScore > defenderScore);

    /* test_progressbar */
    request.setAttribute("gameId", game.getId());
%>

<% if (game.getState() == GameState.FINISHED) { %>
    <%@include file="game_components/finished_modal.jsp"%>
<% } %>

<div class="row" style="padding: 0px 15px;">
    <div class="col-md-6" id="cut-div">
        <h3>Class Under Test</h3>
        <%@include file="game_components/class_viewer.jsp"%>
        <%@include file="game_components/game_highlighting.jsp"%>
        <%@include file="game_components/mutant_explanation.jsp"%>
    </div>

    <div class="col-md-6" id="utest-div">
        <%@include file="game_components/test_progress_bar.jsp"%>
        <h3>Write a new JUnit test here
            <button type="submit" class="btn btn-primary btn-game btn-right" id="submitTest" form="def" onClick="progressBar(); this.form.submit(); this.disabled=true; this.value='Defending...';"
                    <% if (game.getState() != GameState.ACTIVE || game.getActiveRole() != Role.DEFENDER) { %> disabled <% } %>>
                Defend!
            </button>
        </h3>
        <form id="def" action="<%=request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase() %>" method="post">
            <%@include file="game_components/test_editor.jsp"%>
            <input type="hidden" name="formType" value="createTest">
        </form>
    </div>
</div>

<div class="row" style="padding: 0px 15px;">
	<div class="col-md-6" id="submitted-div">
		<h3>JUnit tests </h3>
		<%@include file="game_components/tests_carousel.jsp"%>
	</div>

	<div class="col-md-6" id="mutants-div">
		<h3>Mutants</h3>
        <%@include file="game_components/mutants_list.jsp"%>
	</div>
</div>

<script>
	<% if (game.getActiveRole().equals(Role.ATTACKER)) {%>
        function checkForUpdate(){
            $.post('/play', {
                formType: "whoseTurn",
                gameID: <%= game.getId() %>
            }, function(data){
                if(data === "defender"){
                    window.location.reload();
                }
            },"text");
        }
        setInterval("checkForUpdate()", 10000);
	<% } %>
</script>

<% } %>

<%@include file="/jsp/footer_game.jsp" %>
