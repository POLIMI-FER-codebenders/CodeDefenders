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
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%@ page import="org.codedefenders.game.GameState"%>
<%@ page import="org.codedefenders.game.Role"%>
<%@ page import="org.codedefenders.util.Paths"%>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.game.AbstractGame" %>
<%@ page import="org.codedefenders.game.multiplayer.MeleeGame" %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request" />

<%
    AbstractGame game = (AbstractGame) request.getAttribute("game");
    int gameId = game.getId();

    Role role = null;
    String selectionManagerUrl = null;
    if (game instanceof MeleeGame) {
        selectionManagerUrl = request.getContextPath() + Paths.MELEE_SELECTION;
        role = ((MeleeGame) game).getRole(login.getUserId());
    } else if (game instanceof MultiplayerGame) {
        selectionManagerUrl = request.getContextPath() + Paths.BATTLEGROUND_SELECTION;
        role = ((MultiplayerGame) game).getRole(login.getUserId());
    }
%>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request" />
<% pageInfo.setPageTitle("Game " + game.getId() + " (" + role.getFormattedString() + ")"); %>

<jsp:include page="/jsp/header.jsp" />

<link href="${pageContext.request.contextPath}/css/specific/game.css" rel="stylesheet">

<div id="game-container" class="container-fluid"> <%-- closed in footer --%>
    <div class="d-flex flex-wrap justify-content-between align-items-end gap-3">
        <h2 class="m-0">${pageInfo.pageTitle}</h2>
        <div class="d-flex flex-wrap align-items-center gap-2">

            <%
                if (game.getCreatorId() == login.getUserId()) {
                    if (game.getState() == GameState.ACTIVE) {
            %>
                    <form id="adminEndBtn" action="<%=selectionManagerUrl%>" method="post">
                        <input type="hidden" name="formType" value="endGame">
                        <input type="hidden" name="gameId" value="<%=game.getId()%>">
                        <button type="submit" class="btn btn-sm btn-danger" id="endGame" form="adminEndBtn">
                            End Game
                        </button>
                    </form>
            <%
                    }

                    if (game.getState() == GameState.CREATED) {
            %>
                    <form id="adminStartBtn" action="<%=selectionManagerUrl%>" method="post">
                        <input type="hidden" name="formType" value="startGame">
                        <input type="hidden" name="gameId" value="<%=game.getId()%>">
                        <button type="submit" class="btn btn-sm btn-success" id="startGame" form="adminStartBtn">
                            Start Game
                        </button>
                    </form>
            <%
                    }

                    if (game.getState() == GameState.ACTIVE || game.getState() == GameState.FINISHED) {
            %>
                <div>
                    <div data-bs-toggle="tooltip"
                         title="Start a new game with the same settings and opposite roles.">
                        <button type="submit" class="btn btn-sm btn-warning" id="rematch"
                                data-bs-toggle="modal" data-bs-target="#rematch-modal">
                            Rematch
                        </button>
                    </div>
                    <form id="rematch-form" action="<%=selectionManagerUrl%>" method="post">
                        <input type="hidden" name="formType" value="rematch">
                        <input type="hidden" name="gameId" value="<%=game.getId()%>">
                        <t:modal title="Confirm Rematch" id="rematch-modal" closeButtonText="Cancel">
                            <jsp:attribute name="content">
                                Are you sure you want to create a new game with opposite roles?
                            </jsp:attribute>
                            <jsp:attribute name="footer">
                                <button type="submit" class="btn btn-primary">Confirm Rematch</button>
                            </jsp:attribute>
                        </t:modal>
                    </form>
                </div>
            <%
                    }
                }
            %>

            <div class="btn-group">
                <button class="btn btn-sm btn-outline-secondary text-nowrap" id="btnScoreboard"
                        data-bs-toggle="modal" data-bs-target="#scoreboard">
                    <i class="fa fa-book"></i>
                    Scoreboard
                </button>
                <button class="btn btn-sm btn-outline-secondary" id="btnScoringModal"
                        data-bs-toggle="modal" data-bs-target="#scoringModal">
                    <i class="fa fa-question-circle"></i>
                </button>
            </div>
            <t:modal title="Scoring System" id="scoringModal" modalBodyClasses="bg-light">
                <jsp:attribute name="content">
                    <jsp:include page="/jsp/scoring_system.jsp"/>
                </jsp:attribute>
            </t:modal>

            <button type="button" class="btn btn-sm btn-outline-secondary text-nowrap" id="btnHistory"
                    data-bs-toggle="modal" data-bs-target="#history">
                <i class="fa fa-history"></i>
                Timeline
            </button>

            <a href="<%=request.getContextPath() + Paths.PROJECT_EXPORT%>?gameId=<%=gameId%>"
               class="btn btn-sm btn-outline-secondary text-nowrap" id="btnProjectExport"
               title="Export as a Gradle project to import into an IDE.">
                <i class="fa fa-download"></i>
                Gradle Export
            </a>

            <button type="button" class="btn btn-sm btn-outline-secondary text-nowrap" id="btnFeedback"
                    data-bs-toggle="modal" data-bs-target="#playerFeedback">
                <i class="fa fa-comment"></i>
                Feedback
            </button>

            <jsp:include page="/jsp/game_components/keymap_config.jsp"/>

            <t:game_chat/>
        </div>
    </div>
