/******************************************************************************
 * Copyright (C) 2008 Low Heng Sin                                            *
 * Copyright (C) 2008 Idalica Corporation                                     *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package tw.idempiere.form;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListHeader;
import org.adempiere.webui.component.ListItem;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.WListItemRenderer;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WChosenboxSearchEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.event.DialogEvents;
import org.adempiere.webui.factory.InfoManager;
import org.adempiere.webui.info.InfoWindow;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.StatusBarPanel;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MBPartner;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MTable;
import org.compiere.model.MOrder;
import org.compiere.model.MProduction;
import org.compiere.model.MQuery;
import org.compiere.model.MRefList;
import org.compiere.model.MRole;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.ValueNamePair;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.ValueNamePair;
import org.compiere.wf.MWFActivity;
import org.compiere.wf.MWFNode;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.util.media.AMedia;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Center;
import org.zkoss.zul.North;
import org.zkoss.zul.South;
import org.zkoss.zul.Div;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vlayout;

public class WWFActivityTG extends ADForm implements EventListener<Event> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8405802852868437716L;
	/** Window No */
	private int m_WindowNo = 0;
	/** Open Activities */
	private MWFActivity[] m_activities = null;
	/** Current Activity */
	private MWFActivity m_activity = null;
	/** Current Activity */
	private int m_index = 0;
	/** Set Column */
	private MColumn m_column = null;
	/** Logger */
	private static CLogger log = CLogger.getCLogger(WWFActivityTG.class);

	private static final String VERSION = "v2.0";

	//
	private Label lNode = new Label(Msg.translate(Env.getCtx(), "AD_WF_Node_ID"));
	private Textbox fNode = new Textbox();
	private Label lDesctiption = new Label(Msg.translate(Env.getCtx(), "Description"));
	private Textbox fDescription = new Textbox();
	private Label lHelp = new Label(Msg.translate(Env.getCtx(), "Help"));
	private Textbox fHelp = new Textbox();
	private Label lHistory = new Label(Msg.translate(Env.getCtx(), "Description"));
	private Html fHistory = new Html();
	private Label lAnswer = new Label(Msg.getMsg(Env.getCtx(), "Answer"));
	private Textbox fAnswerText = new Textbox();
	private Listbox fAnswerList = new Listbox();
	private Button fAnswerButton = new Button();
	private Button bZoom = new Button();
	private Label lTextMsg = new Label(Msg.getMsg(Env.getCtx(), "Comment"));
	private Textbox fTextMsg = new Textbox();
	private Button bOK = new Button();
	private Toolbarbutton bBatchApprove = new Toolbarbutton(); // Batch Approve
	private Button bSplit = new Button(); // 拆單：依 SysConfig TABLE_SUPPORT_SPLIT 對應的單據才顯示
	private int m_splitInfoWindowID = 0; // 目前單據對應的拆單 Info Window（0=不顯示）
	private Button bAttachment = new Button(); // 唯讀檢視當前單據附件；依附件數啟用
	private WSearchEditor fForward = null; // dynInit
	private Label lForward = new Label(Msg.getMsg(Env.getCtx(), "Forward"));
	private Label lOptional = new Label("(" + Msg.translate(Env.getCtx(), "Optional") + ")");
	private MLookup m_userLookup = null; // 同轉發的使用者來源（供會簽人多選清單）

	// 會簽指定（僅在「同意後會進會簽節點」時顯示）
	private static final String NODE_VALUE_COUNTERSIGN = "CounterSign";
	private Row rowActionCs = new Row();
	private Label lCsUsers = new Label("會簽人員");
	private WChosenboxSearchEditor fCsUsers = null; // dynInit
	private Label lCsRoles = new Label("會簽角色");
	private WChosenboxSearchEditor fCsRoles = null; // dynInit
	private StatusBarPanel statusBar = new StatusBarPanel();

	private ListModelTable model = null;
	private WListbox listbox = new WListbox();

	private final static String HISTORY_DIV_START_TAG = "<div style='min-height: 100px; max-height: 320px; overflow: auto; border: 1px solid #7F9DB9;'>";

	public WWFActivityTG() {
		super();
		LayoutUtils.addSclass("workflow-activity-form", this);
	}

	protected void initForm() {
		loadActivities();

		fAnswerList.setMold("select");

		if (ThemeManager.isUseFontIconForImage())
			bZoom.setIconSclass("z-icon-Zoom");
		else
			bZoom.setImage(ThemeManager.getThemeResource("images/Zoom16.png"));
		if (ThemeManager.isUseFontIconForImage())
			bOK.setIconSclass("z-icon-Ok");
		else
			bOK.setImage(ThemeManager.getThemeResource("images/Ok16.png"));

		bBatchApprove.setLabel(Msg.getMsg(Env.getCtx(), "Batch Approve"));
		bBatchApprove.addEventListener(Events.ON_CLICK, this);

		if (ThemeManager.isUseFontIconForImage())
			bBatchApprove.setIconSclass("z-icon-Process");
		else
			bBatchApprove.setImage(ThemeManager.getThemeResource("images/Process16.png"));

		// 部分簽核按鈕：預設隱藏，由 display() 依當前單據是否在 TABLE_SUPPORT_SPLIT 白名單決定顯示
		bSplit.setLabel("部分簽核");
		bSplit.addEventListener(Events.ON_CLICK, this);
		if (ThemeManager.isUseFontIconForImage())
			bSplit.setIconSclass("z-icon-Process");
		else
			bSplit.setImage(ThemeManager.getThemeResource("images/Process16.png"));
		bSplit.setVisible(false);

		// 附件按鈕：唯讀檢視；預設停用，由 display() 依附件數啟用並標數量
		bAttachment.setLabel("附件");
		bAttachment.addEventListener(Events.ON_CLICK, this);
		if (ThemeManager.isUseFontIconForImage())
			bAttachment.setIconSclass("z-icon-Attachment");
		else
			bAttachment.setImage(ThemeManager.getThemeResource("images/Attachment16.png"));
		bAttachment.setDisabled(true);

		m_userLookup = MLookupFactory.get(Env.getCtx(), m_WindowNo,
				0, 10443, DisplayType.Search);
		fForward = new WSearchEditor(m_userLookup, Msg.translate(
				Env.getCtx(), "AD_User_ID"), "", true, false, true);

		// 會簽人/角色：Chosen Multiple Selection Search（打字搜尋、chip 多選）
		try {
			fCsUsers = new WChosenboxSearchEditor(m_userLookup, "會簽人員", "", false, false, true);
			String roleVal = "AD_Role.AD_Role_ID IN (SELECT AD_Role_ID FROM AD_WF_Responsible"
					+ " WHERE ResponsibleType='R' AND IsActive='Y' AND AD_Role_ID IS NOT NULL)";
			// 用 TableDir 從 ColumnName 推出 AD_Role 表（Search 型無法只憑 ColumnName 合成）；
			// 搜尋體驗由 WChosenboxSearchEditor 提供，與 lookup 型別無關。
			MLookup roleLookup = MLookupFactory.get(Env.getCtx(), m_WindowNo, 0, DisplayType.TableDir,
					Env.getLanguage(Env.getCtx()), "AD_Role_ID", 0, false, roleVal);
			fCsRoles = new WChosenboxSearchEditor(roleLookup, "會簽角色", "", false, false, true);
		} catch (Exception e) {
			log.log(Level.SEVERE, "建立會簽選人元件失敗", e);
		}

		init();
		display(-1);
	}

	private void init() {
		// Main Layout
		Borderlayout layout = new Borderlayout();
		ZKUpdateUtil.setWidth(layout, "100%");
		ZKUpdateUtil.setHeight(layout, "100%");
		layout.setStyle("background-color: transparent; position: absolute;");

		// --- North Region (Toolbar + List) ---
		North north = new North();
		north.setSplittable(true);
		ZKUpdateUtil.setHeight(north, "30%");
		north.setStyle("background-color: transparent");

		Vlayout northLayout = new Vlayout();
		northLayout.setVflex("1");
		northLayout.setHflex("1");

		// Toolbar
		Toolbar toolbar = new Toolbar();
		toolbar.setHflex("1");
		toolbar.setHeight("36px");
		// Version Label in Toolbar
		Label lVersionToolbar = new Label(VERSION);
		lVersionToolbar.setStyle("float: right; margin-top: 8px; margin-right: 10px; font-size: 10px; color: #888;");
		toolbar.appendChild(lVersionToolbar);

		// Program Name
		Label lProgramName = new Label(Msg.getMsg(Env.getCtx(), "Nexus Approval Hub"));
		lProgramName.setStyle(
				"float: right; margin-top: 8px; margin-right: 8px; font-size: 12px; font-weight: bold; color: #555;");
		toolbar.appendChild(lProgramName);

		Label space = new Label(" ");
		toolbar.appendChild(space);
		toolbar.appendChild(bBatchApprove);

		// Motto
		Label lMotto = new Label(Msg.getMsg(Env.getCtx(), "Decisions Today Shape Tomorrow"));
		lMotto.setStyle("margin-left: 15px; font-style: italic; color: #777; vertical-align: middle;");
		toolbar.appendChild(lMotto);

		northLayout.appendChild(toolbar);

		// Listbox
		ZKUpdateUtil.setVflex(listbox, "1");
		ZKUpdateUtil.setHflex(listbox, "1");
		northLayout.appendChild(listbox);

		north.appendChild(northLayout);
		// Set Header Background
		north.setStyle("background-image: url(data:image/jpeg;base64," + WWFActivityImage.HEADER_BG_BASE64
				+ "); background-size: cover; background-repeat: no-repeat; background-position: center; border-bottom: 1px solid #ccc;");
		layout.appendChild(north);
		listbox.addEventListener(Events.ON_SELECT, this);

		// --- Center Region (Activity Details & Actions) ---
		Center center = new Center();
		center.setAutoscroll(true);
		center.setStyle("background-color: transparent");

		Vlayout centerLayout = new Vlayout();
		centerLayout.setSpacing("10px");
		centerLayout.setStyle("padding: 10px;");
		ZKUpdateUtil.setWidth(centerLayout, "100%");

		// Group 1: Workflow Activity Information
		Groupbox gbInfo = new Groupbox();
		gbInfo.appendChild(new Caption(Msg.translate(Env.getCtx(), "Workflow")));
		gbInfo.setMold("3d");

		Grid gridInfo = new Grid();
		gridInfo.makeNoStrip();
		gridInfo.setOddRowSclass("even");
		ZKUpdateUtil.setWidth(gridInfo, "100%");

		Rows rowsInfo = new Rows();
		gridInfo.appendChild(rowsInfo);

		// Row 1: Node + Version
		Row rowInfo1 = new Row();
		rowInfo1.appendChild(lNode);
		rowInfo1.appendChild(fNode);
		ZKUpdateUtil.setHflex(fNode, "true");
		fNode.setReadonly(true);

		rowInfo1.appendChild(new Label()); // Spacer
		rowsInfo.appendChild(rowInfo1);

		// Row 2: Description
		Row rowInfo2 = new Row();
		rowInfo2.appendChild(lDesctiption);
		rowInfo2.appendChild(fDescription);
		ZKUpdateUtil.setHflex(fDescription, "true");
		fDescription.setMultiline(true);
		fDescription.setRows(2);
		fDescription.setReadonly(true);
		rowInfo2.appendChild(new Label());
		rowsInfo.appendChild(rowInfo2);

		// Row 3: Help
		Row rowInfo3 = new Row();
		rowInfo3.appendChild(lHelp);
		rowInfo3.appendChild(fHelp);
		ZKUpdateUtil.setHflex(fHelp, "true");
		fHelp.setMultiline(true);
		fHelp.setRows(2);
		fHelp.setReadonly(true);
		rowInfo3.appendChild(new Label());
		rowsInfo.appendChild(rowInfo3);

		gbInfo.appendChild(gridInfo);
		centerLayout.appendChild(gbInfo);

		// Group 2: History & Message
		Groupbox gbHistory = new Groupbox();
		gbHistory.appendChild(new Caption("Abstract Message"));
		gbHistory.setMold("3d");

		ZKUpdateUtil.setWidth(fHistory, "100%");
		ZKUpdateUtil.setHeight(fHistory, "150px");
		Div divHistory = new Div();
		divHistory.setStyle("overflow: auto; border: 1px solid #ccc; background: white; padding: 5px;");
		divHistory.setHeight("150px");
		divHistory.appendChild(fHistory);
		gbHistory.appendChild(divHistory);

		centerLayout.appendChild(gbHistory);

		// Group 3: Actions
		Groupbox gbAction = new Groupbox();
		gbAction.appendChild(new Caption(Msg.translate(Env.getCtx(), "Action")));
		gbAction.setMold("3d");

		Grid gridAction = new Grid();
		gridAction.makeNoStrip();
		gridAction.setOddRowSclass("even");
		ZKUpdateUtil.setWidth(gridAction, "100%");

		Rows rowsAction = new Rows();
		gridAction.appendChild(rowsAction);

		// Row 1: Answer
		Row rowAction1 = new Row();
		rowAction1.appendChild(lAnswer);
		Hbox hboxAnswer = new Hbox();
		hboxAnswer.setHflex("1");
		hboxAnswer.setAlign("center");
		hboxAnswer.appendChild(fAnswerText);
		ZKUpdateUtil.setHflex(fAnswerText, "1");
		hboxAnswer.appendChild(fAnswerList); // Listbox mold=select
		hboxAnswer.appendChild(fAnswerButton);
		fAnswerButton.setIconSclass("z-icon-Go");
		fAnswerButton.addEventListener(Events.ON_CLICK, this);
		hboxAnswer.appendChild(new Separator("vertical"));
		hboxAnswer.appendChild(bZoom);
		bZoom.addEventListener(Events.ON_CLICK, this);
		hboxAnswer.appendChild(new Separator("vertical"));
		hboxAnswer.appendChild(bAttachment);
		rowAction1.appendChild(hboxAnswer);
		rowAction1.appendChild(new Label());
		rowsAction.appendChild(rowAction1);

		// Row 1.5: 會簽指定（人/角色 多選；預設隱藏，由 display() 決定）
		rowActionCs.appendChild(lCsUsers);
		Hbox hboxCs = new Hbox();
		hboxCs.setHflex("1");
		if (fCsUsers != null)
			hboxCs.appendChild(fCsUsers.getComponent());
		hboxCs.appendChild(new Separator("vertical"));
		hboxCs.appendChild(lCsRoles);
		if (fCsRoles != null)
			hboxCs.appendChild(fCsRoles.getComponent());
		rowActionCs.appendChild(hboxCs);
		rowActionCs.appendChild(new Label());
		rowActionCs.setVisible(false);
		rowsAction.appendChild(rowActionCs);

		// Row 2: Comment
		Row rowAction2 = new Row();
		rowAction2.appendChild(lTextMsg);
		rowAction2.appendChild(fTextMsg);
		ZKUpdateUtil.setHflex(fTextMsg, "true");
		fTextMsg.setMultiline(true);
		fTextMsg.setRows(2);
		rowAction2.appendChild(new Label());
		rowsAction.appendChild(rowAction2);

		// Row 3: Forward & Process
		Row rowAction3 = new Row();
		rowAction3.appendChild(lForward);
		Hbox hboxForward = new Hbox();
		hboxForward.setHflex("1");
		hboxForward.setAlign("center");
		hboxForward.appendChild(fForward.getComponent());
		hboxForward.appendChild(new Separator("vertical"));
		hboxForward.appendChild(lOptional);
		rowAction3.appendChild(hboxForward);

		// Button OK + 部分簽核（同一格，部分簽核在 OK 右邊）
		bOK.addEventListener(Events.ON_CLICK, this);
		// Reset styling just in case
		bOK.setStyle(null);
		Hbox hboxOK = new Hbox();
		hboxOK.setAlign("center");
		hboxOK.appendChild(bOK);
		hboxOK.appendChild(new Separator("vertical"));
		hboxOK.appendChild(bSplit);
		rowAction3.appendChild(hboxOK);

		rowsAction.appendChild(rowAction3);

		gbAction.appendChild(gridAction);
		centerLayout.appendChild(gbAction);

		center.appendChild(centerLayout);
		layout.appendChild(center);

		// South Region (Status Bar)
		South south = new South();
		south.appendChild(statusBar);
		south.setStyle("background-color: transparent");
		layout.appendChild(south);

		this.appendChild(layout);
		this.setStyle("height: 100%; width: 100%; position: absolute;");
	}

	public void onEvent(Event event) throws Exception {
		Component comp = event.getTarget();
		String eventName = event.getName();

		if (eventName.equals(Events.ON_CLICK)) {
			if (comp == bZoom)
				cmd_zoom();
			else if (comp == bOK) {
				Clients.showBusy(Msg.getMsg(Env.getCtx(), "Processing"));
				Events.echoEvent("onOK", this, null);
			} else if (comp == bBatchApprove) {
				cmd_batchApprove();
			} else if (comp == bSplit) {
				cmd_split();
			} else if (comp == bAttachment) {
				cmd_attachment();
			} else if (comp == fAnswerButton)

				cmd_button();
		} else if (Events.ON_SELECT.equals(eventName) && comp == listbox) {
			m_index = listbox.getSelectedIndex();
			if (m_index >= 0)
				display(m_index);
		}
		// fAnswerList
		else if (Events.ON_SELECT.equals(eventName) && comp == fAnswerList) {
			if (fAnswerList.getSelectedItem().getValue().equals("N")
					&& fTextMsg.getText().length() == 0)
				Clients.showNotification(
						Msg.translate(Env.getCtx(), "RejectReasons"), fTextMsg);
		} else {
			super.onEvent(event);
		}
	}

	/**
	 * Get active activities count
	 * 
	 * @return int
	 */
	public int getActivitiesCount() {
		int count = 0;

		String sql = "SELECT COUNT(*) FROM AD_WF_Activity a "
				+ "WHERE " + getWhereActivities();
		int AD_User_ID = Env.getAD_User_ID(Env.getCtx());
		int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
		MRole role = MRole.get(Env.getCtx(), Env.getAD_Role_ID(Env.getCtx()));
		sql = role.addAccessSQL(sql, "a", true, false);
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, AD_User_ID);
			pstmt.setInt(2, AD_User_ID);
			pstmt.setInt(3, AD_User_ID);
			pstmt.setInt(4, AD_User_ID);
			pstmt.setInt(5, AD_User_ID);
			pstmt.setInt(6, AD_User_ID); // 代簽：代理人(登入者)
			pstmt.setInt(7, AD_Client_ID);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				count = rs.getInt(1);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, sql, e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return count;

	}

	/**
	 * Load Activities
	 * 
	 * @return int
	 */
	public int loadActivities() {
		long start = System.currentTimeMillis();

		int MAX_ACTIVITIES_IN_LIST = MSysConfig.getIntValue(MSysConfig.MAX_ACTIVITIES_IN_LIST, 200,
				Env.getAD_Client_ID(Env.getCtx()));

		model = new ListModelTable();

		ArrayList<MWFActivity> list = new ArrayList<MWFActivity>();
		// 代簽：登入者目前可代簽的(請假中)主管集合 + 開放代簽的單據集合，供清單逐列標示用（查一次，避免每列打 DB）
		Set<Integer> delegatedSupIds = getDelegatedSupervisorIDs();
		Set<Integer> delegationTableIds = getDelegationTableIDs();
		String sql = "SELECT * FROM AD_WF_Activity a "
				+ "WHERE " + getWhereActivities()
				+ " ORDER BY a.Priority DESC, Created";
		int AD_User_ID = Env.getAD_User_ID(Env.getCtx());
		int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
		MRole role = MRole.get(Env.getCtx(), Env.getAD_Role_ID(Env.getCtx()));
		sql = role.addAccessSQL(sql, "a", true, false);
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, AD_User_ID);
			pstmt.setInt(2, AD_User_ID);
			pstmt.setInt(3, AD_User_ID);
			pstmt.setInt(4, AD_User_ID);
			pstmt.setInt(5, AD_User_ID);
			pstmt.setInt(6, AD_User_ID); // 代簽：代理人(登入者)
			pstmt.setInt(7, AD_Client_ID);

			rs = pstmt.executeQuery();
			while (rs.next()) {
				MWFActivity activity = new MWFActivity(Env.getCtx(), rs, null);
				list.add(activity);
				List<Object> rowData = new ArrayList<Object>();

				// 0. 代簽標示：待辦 owner 為登入者可代簽的(請假中)主管時，顯示「代 XXX 主管」
				String delegateMark = "";
				int ownerId = activity.getAD_User_ID();
				if (ownerId > 0 && delegatedSupIds.contains(ownerId)
						&& delegationTableIds.contains(activity.getAD_Table_ID())) {
					MUser sup = MUser.get(Env.getCtx(), ownerId);
					delegateMark = "代 " + (sup != null ? sup.getName() : ("#" + ownerId)) + " 主管";
				}
				rowData.add(delegateMark);

				// 1. User/Contact
				String userName = "";
				// User 改成 單據 （Table）的 AD_User_ID ，若沒有這個欄位 抓 Createdby
				PO po = activity.getPO();
				if (po != null) {
					int userID = 0;
					int idx = po.get_ColumnIndex("AD_User_ID");
					if (idx >= 0) {
						Object val = po.get_Value(idx);
						if (val != null && val instanceof Integer)
							userID = (Integer) val;
					}
					if (userID <= 0)
						userID = po.getCreatedBy();

					if (userID > 0) {
						MUser user = MUser.get(Env.getCtx(), userID);
						if (user != null)
							userName = user.getName();
					}
				}
				if (userName == null || userName.length() == 0) {
					if (activity.getAD_User_ID() > 0 && activity.getAD_User() != null)
						userName = activity.getAD_User().getName();
				}
				rowData.add(userName);

				// 2. Document Type
				String docType = "";
				if (activity.getAD_Table_ID() > 0 && activity.getAD_Table() != null) {
					docType = activity.getAD_Table().getDescription();
					if (docType == null || docType.trim().length() == 0)
						docType = activity.getAD_Table().getName();
				}
				rowData.add(docType);

				// 3. Workflow Node
				rowData.add(activity.getNodeName());

				// 4. Description
				rowData.add(getDescription(activity));

				// 5. Summary
				String docSummary = getSummary(activity);
				if (docSummary != null)
					rowData.add(docSummary);
				else
					rowData.add(activity.getSummary());

				model.add(rowData);
				if (list.size() > MAX_ACTIVITIES_IN_LIST && MAX_ACTIVITIES_IN_LIST > 0) {
					log.warning("More then 200 Activities - ignored");
					break;
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, sql, e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		m_activities = new MWFActivity[list.size()];
		list.toArray(m_activities);
		//
		if (log.isLoggable(Level.FINE))
			log.fine("#" + m_activities.length
					+ "(" + (System.currentTimeMillis() - start) + "ms)");
		m_index = 0;

		String[] columns = new String[] {
				"代簽",
				Msg.translate(Env.getCtx(), "AD_User_ID"),
				Msg.translate(Env.getCtx(), "DocumentType"),
				Msg.translate(Env.getCtx(), "AD_WF_Node_ID"),
				Msg.translate(Env.getCtx(), "Description"),
				Msg.translate(Env.getCtx(), "Summary") };

		WListItemRenderer renderer = new WListItemRenderer(Arrays.asList(columns));
		ListHeader header = new ListHeader();
		ZKUpdateUtil.setWidth(header, null);
		renderer.setListHeader(0, header);
		header = new ListHeader();
		ZKUpdateUtil.setWidth(header, null);
		renderer.setListHeader(1, header);
		header = new ListHeader();
		ZKUpdateUtil.setWidth(header, null);
		renderer.setListHeader(2, header);
		header = new ListHeader();
		ZKUpdateUtil.setWidth(header, null);
		renderer.setListHeader(3, header);
		header = new ListHeader();
		ZKUpdateUtil.setWidth(header, null);
		renderer.setListHeader(4, header);
		header = new ListHeader();
		ZKUpdateUtil.setWidth(header, null);
		renderer.setListHeader(5, header);

		renderer.addTableValueChangeListener(listbox);
		model.setNoColumns(columns.length);
		listbox.setModel(model);
		listbox.setItemRenderer(renderer);
		listbox.setMultiple(true);
		listbox.setCheckmark(true);
		listbox.repaint();

		listbox.setSizedByContent(false);

		return m_activities.length;
	} // loadActivities

	private String getDescription(MWFActivity activity) {
		PO po = activity.getPO();
		String description = null;
		if (po != null) {
			int idx = po.get_ColumnIndex("Description");
			if (idx >= 0) {
				Object val = po.get_Value(idx);
				if (val != null)
					description = val.toString();
			}
		}
		if (description == null || description.trim().length() == 0) {
			description = activity.getTextMsg();
		}
		return description;
	}

	private String getSummary(MWFActivity activity) {
		// 1) 工單單號 + 對象 + 三角交易
		if (activity.getAD_Table_ID() == MProduction.Table_ID) {
			StringBuilder sb = new StringBuilder();
			MProduction mo = new MProduction(activity.getCtx(), activity.getRecord_ID(), activity.get_TrxName());
			sb.append(Msg.getElement(activity.getCtx(), "M_Production_ID")).append(" ");
			sb.append(mo.getDocumentNo()).append(" ");

			if (mo.getC_OrderLine_ID() > 0 && mo.getC_OrderLine().getC_Order_ID() > 0) {
				MOrder so = (MOrder) mo.getC_OrderLine().getC_Order();
				if (so.isDropShip() && so.getDropShip_BPartner_ID() > 0) {
					sb.append(so.getC_BPartner().getName2()).append(": ");
					sb.append(so.getDropShip_BPartner().getName()).append(" ");
				} else {
					sb.append(so.getC_BPartner().getName()).append(" ");
				}
			}
			return sb.toString();
		}

		return null;
	}

	private String getWhereActivities() {
		final String where = "a.Processed='N' AND a.WFState='OS' "
				+ " AND (EXISTS (select * from AD_WF_Node wfn where "
				+ " a.AD_WF_Node_ID = wfn.AD_WF_Node_ID "
				+ " and  not ( substring(wfn.name,1,8) = '[SYSTEM]' and wfn.action = 'Z' ) ) )"

				+ " AND ("
				// Owner of Activity
				+ " a.AD_User_ID=?" // #1
				// Invoker (if no invoker = all)
				+ " OR EXISTS (SELECT * FROM AD_WF_Responsible r WHERE a.AD_WF_Responsible_ID=r.AD_WF_Responsible_ID"
				+ " AND r.ResponsibleType='H' AND COALESCE(r.AD_User_ID,0)=0 AND COALESCE(r.AD_Role_ID,0)=0 AND (a.AD_User_ID=? OR a.AD_User_ID IS NULL))" // #2
				// Responsible User
				+ " OR EXISTS (SELECT * FROM AD_WF_Responsible r WHERE a.AD_WF_Responsible_ID=r.AD_WF_Responsible_ID"
				+ " AND r.ResponsibleType='H' AND r.AD_User_ID=?)" // #3
				// Responsible Role
				+ " OR EXISTS (SELECT * FROM AD_WF_Responsible r INNER JOIN AD_User_Roles ur ON (r.AD_Role_ID=ur.AD_Role_ID)"
				+ " WHERE a.AD_WF_Responsible_ID=r.AD_WF_Responsible_ID AND r.ResponsibleType='R' AND ur.AD_User_ID=? AND ur.isActive = 'Y')" // #4
				/// * Manual Responsible */
				+ " OR EXISTS (SELECT * FROM AD_WF_ActivityApprover r "
				+ " WHERE a.AD_WF_Activity_ID=r.AD_WF_Activity_ID AND r.AD_User_ID=? AND r.isActive = 'Y')" // #5
				// 代簽：待辦 owner 為請假中(已核准且涵蓋今天)主管，且登入者是其代理人
				+ " OR EXISTS (SELECT 1 FROM AD_User u"
				+ " JOIN HR_AbsenceNote ab ON ab.AD_User_ID = u.AD_User_ID"
				+ " WHERE u.AD_User_ID = a.AD_User_ID AND u.Substitute_User_ID = ?" // #6 代理人(登入者)
				+ " AND u.IsActive='Y' AND ab.DocStatus='CO'"
				+ " AND current_date BETWEEN ab.DateAbsenceFrom::date AND ab.DateAbsenceTo::date"
				+ " AND a.AD_Table_ID IN (" + delegationTableIDsCsv() + "))" // 限白名單單據
				+ ") AND a.AD_Client_ID=?"; // #7
		return where;
	}

	/**
	 * 代簽判定：若這張待辦的 owner 是「請假中(已核准且涵蓋今天)且設定登入者為代理人」的主管，
	 * 回傳該主管 AD_User_ID；否則回傳 0。供簽核留痕標註用。
	 */
	private int getDelegatedSupervisorID(MWFActivity activity) {
		if (activity == null)
			return 0;
		if (!getDelegationTableIDs().contains(activity.getAD_Table_ID()))
			return 0; // 此單據未開放代簽
		int ownerID = activity.getAD_User_ID();
		int meID = Env.getAD_User_ID(Env.getCtx());
		if (ownerID <= 0 || ownerID == meID)
			return 0;
		String sql = "SELECT u.AD_User_ID FROM AD_User u"
				+ " JOIN HR_AbsenceNote ab ON ab.AD_User_ID = u.AD_User_ID"
				+ " WHERE u.AD_User_ID=? AND u.Substitute_User_ID=? AND u.IsActive='Y'"
				+ " AND ab.DocStatus='CO'"
				+ " AND current_date BETWEEN ab.DateAbsenceFrom::date AND ab.DateAbsenceTo::date";
		int supId = DB.getSQLValue(null, sql, ownerID, meID);
		return supId > 0 ? supId : 0;
	}

	/**
	 * 由 SysConfig {@code TABLE_SUPPORT_DELEGATE}（逗號分隔 table 名）解析出「允許代簽」的
	 * AD_Table_ID 集合。預設空字串＝沒有任何單據開放代簽。
	 */
	private Set<Integer> getDelegationTableIDs() {
		Set<Integer> ids = new HashSet<Integer>();
		String csv = MSysConfig.getValue("TABLE_SUPPORT_DELEGATE", "");
		if (csv == null || csv.trim().length() == 0)
			return ids;
		for (String name : csv.split(",")) {
			name = name.trim();
			if (name.length() == 0)
				continue;
			int tableId = MTable.getTable_ID(name);
			if (tableId > 0)
				ids.add(tableId);
		}
		return ids;
	}

	/** 供 SQL 內嵌用：允許代簽的 AD_Table_ID 清單字串（皆為整數，安全內嵌）；無則回 "-1"（永不命中）。 */
	private String delegationTableIDsCsv() {
		StringBuilder sb = new StringBuilder();
		for (int id : getDelegationTableIDs()) {
			if (sb.length() > 0)
				sb.append(",");
			sb.append(id);
		}
		return sb.length() > 0 ? sb.toString() : "-1";
	}

	/**
	 * 登入者目前可代簽的主管集合：設定登入者為代理人(Substitute_User_ID)、且有涵蓋今天的已核准假單者。
	 * 查一次供清單逐列比對，避免每列各打一次 DB。
	 */
	private Set<Integer> getDelegatedSupervisorIDs() {
		int meID = Env.getAD_User_ID(Env.getCtx());
		String sql = "SELECT DISTINCT u.AD_User_ID FROM AD_User u"
				+ " JOIN HR_AbsenceNote ab ON ab.AD_User_ID = u.AD_User_ID"
				+ " WHERE u.Substitute_User_ID=? AND u.IsActive='Y'"
				+ " AND ab.DocStatus='CO'"
				+ " AND current_date BETWEEN ab.DateAbsenceFrom::date AND ab.DateAbsenceTo::date";
		Set<Integer> ids = new HashSet<Integer>();
		for (int id : DB.getIDsEx(null, sql, meID))
			ids.add(id);
		return ids;
	}

	/** 若這張待辦是代簽，於 textMsg 前加「[代簽] 代 XXX 簽核」標註；否則原樣回傳。 */
	private String applyDelegateTag(MWFActivity activity, String textMsg) {
		int supId = getDelegatedSupervisorID(activity);
		if (supId <= 0)
			return textMsg;
		MUser sup = MUser.get(Env.getCtx(), supId);
		String tag = "[代簽] 代 " + (sup != null ? sup.getName() : ("#" + supId)) + " 簽核";
		return (textMsg == null || textMsg.trim().length() == 0) ? tag : tag + " - " + textMsg;
	}

	/**
	 * Reset Display
	 * 
	 * @param selIndex select index
	 * @return selected activity
	 */
	private MWFActivity resetDisplay(int selIndex) {
		fAnswerText.setVisible(false);
		fAnswerList.setVisible(false);
		fAnswerButton.setVisible(false);
		if (ThemeManager.isUseFontIconForImage())
			fAnswerButton.setIconSclass("z-icon-Window");
		else
			fAnswerButton.setImage(ThemeManager.getThemeResource("images/mWindow.png"));
		fTextMsg.setReadonly(!(selIndex >= 0));
		bZoom.setEnabled(selIndex >= 0);
		bOK.setEnabled(selIndex >= 0);
		// 拆單按鈕預設隱藏，由 display() 視當前單據決定
		bSplit.setVisible(false);
		m_splitInfoWindowID = 0;
		// 附件按鈕預設停用，由 display() 依附件數啟用
		bAttachment.setLabel("附件");
		bAttachment.setDisabled(true);
		fForward.setValue(null);
		fForward.setReadWrite(selIndex >= 0);
		// 會簽指定區塊預設隱藏並清空，由 display() 視節點決定是否顯示
		rowActionCs.setVisible(false);
		if (fCsUsers != null)
			fCsUsers.setValue(null);
		if (fCsRoles != null)
			fCsRoles.setValue(null);
		//
		statusBar.setStatusDB(String.valueOf(selIndex + 1) + "/" + m_activities.length);
		m_activity = null;
		m_column = null;
		if (m_activities.length > 0) {
			if (selIndex >= 0 && selIndex < m_activities.length)
				m_activity = m_activities[selIndex];
		}
		// Nothing to show
		if (m_activity == null) {
			fNode.setText("");
			fDescription.setText("");
			fHelp.setText("");
			fHistory.setContent(HISTORY_DIV_START_TAG + "&nbsp;</div>");
			statusBar.setStatusDB("0/0");
			statusBar.setStatusLine(Msg.getMsg(Env.getCtx(), "WFNoActivities"));
		}
		return m_activity;
	} // resetDisplay

	/**
	 * Display.
	 * Fill Editors
	 */
	public void display(int index) {
		if (log.isLoggable(Level.FINE))
			log.fine("Index=" + index);
		//
		m_activity = resetDisplay(index);
		// Nothing to show
		if (m_activity == null) {
			return;
		}
		// Display Activity
		fNode.setText(m_activity.getNodeName());
		fDescription.setValue(m_activity.getNodeDescription());
		fHelp.setValue(m_activity.getNodeHelp());

		fHistory.setContent(getDocumentInfoContent());
		//
		// fHistory.setContent
		// (HISTORY_DIV_START_TAG+m_activity.getHistoryHTML()+"</div>");

		// User Actions
		MWFNode node = m_activity.getNode();
		if (MWFNode.ACTION_UserChoice.equals(node.getAction())) {
			if (m_column == null)
				m_column = node.getColumn();
			if (m_column != null && m_column.get_ID() != 0) {
				fAnswerList.removeAllItems();
				int dt = m_column.getAD_Reference_ID();
				if (dt == DisplayType.YesNo) {
					ValueNamePair[] values = MRefList.getList(Env.getCtx(), 319, false); // _YesNo
					for (int i = 0; i < values.length; i++) {
						fAnswerList.appendItem(values[i].getName(), values[i].getValue());
					}
					fAnswerList.setVisible(true);
				} else if (dt == DisplayType.List) {
					ValueNamePair[] values = MRefList.getList(Env.getCtx(), m_column.getAD_Reference_Value_ID(), false);
					for (int i = 0; i < values.length; i++) {
						fAnswerList.appendItem(values[i].getName(), values[i].getValue());
					}
					fAnswerList.setSelectedIndex(0);
					fAnswerList.setVisible(true);
				} else // other display types come here
				{
					fAnswerText.setText("");
					fAnswerText.setVisible(true);
				}
			}
		}
		// --
		else if (MWFNode.ACTION_UserWindow.equals(node.getAction())
				|| MWFNode.ACTION_UserForm.equals(node.getAction())
				|| MWFNode.ACTION_UserInfo.equals(node.getAction())) {
			fAnswerButton.setLabel(node.getName());
			fAnswerButton.setTooltiptext(node.getDescription());
			fAnswerButton.setVisible(true);
		} else
			log.log(Level.SEVERE, "Unknown Node Action: " + node.getAction());

		// 此節點同意後會進會簽 node → 顯示會簽指定（人/角色）
		if (leadsToCountersign(node)) {
			populateCountersignPickers();
			rowActionCs.setVisible(true);
		}

		// 部分簽核：當前單據在 TABLE_SUPPORT_SPLIT 白名單、且這筆不是會簽 node 時才顯示按鈕。
		// 會簽人員只是其中一個平行簽核者，無權拆單，故會簽 node 上不顯示。
		boolean isCountersignNode = NODE_VALUE_COUNTERSIGN.equals(node.getValue());
		m_splitInfoWindowID = getSplitInfoWindowID(m_activity.getAD_Table_ID());
		bSplit.setVisible(m_splitInfoWindowID > 0 && !isCountersignNode);

		// 附件：有附件才啟用，並在標籤標數量
		MAttachment att = MAttachment.get(Env.getCtx(), m_activity.getAD_Table_ID(), m_activity.getRecord_ID());
		int attCount = (att != null) ? att.getEntryCount() : 0;
		bAttachment.setLabel(attCount > 0 ? "附件(" + attCount + ")" : "附件");
		bAttachment.setDisabled(attCount == 0);

		statusBar.setStatusDB((m_index + 1) + "/" + m_activities.length);
		statusBar.setStatusLine(Msg.getMsg(Env.getCtx(), "WFActivities"));
	} // display

	private String getDocumentInfoContent() {
		// 1. Check for Abstract Message (Highest Priority)
		// 改撈單據的 AbstractMessage 虛擬欄位（透過載入 PO，ColumnSQL 於載入時運算）：
		// 一般表的虛擬欄位仍指向 AD_WorkflowAbstractMessage（stored）；請購單則指向 requisitionabstractmessage() 即時函式。
		// 比照會簽 getCountersignContent() 的即時作法，並與 @AbstractMessage@ 郵件同一來源（虛擬欄位）。
		PO po = MTable.get(Env.getCtx(), m_activity.getAD_Table_ID()).getPO(m_activity.getRecord_ID(), null);
		if (po != null && po.get_ColumnIndex("AbstractMessage") >= 0) {
			Object v = po.get_Value("AbstractMessage");
			String abstractMsg = (v != null) ? v.toString() : null;
			if (abstractMsg != null && abstractMsg.trim().length() > 0) {
				return HISTORY_DIV_START_TAG + abstractMsg.replaceAll("\n", "<br />")
						+ getCountersignContent() + getApprovalCommentContent() + "</div>";
			}
		}

		// 2. Check for Table Support TextMsg
		String tableName = m_activity.getAD_Table().getTableName();
		String avaiableTableList = MSysConfig.getValue("TABLE_SUPPORT_TEXTMSG",
				"HR_OvertimeNote,HR_AbsenceNote,M_WorkshopTankConfirm");

		if (avaiableTableList.contains(tableName)) {
			String sql = "SELECT TextMsg FROM " + tableName + " WHERE " + tableName + "_ID=?";
			String textMsg = DB.getSQLValueString(null, sql, m_activity.getRecord_ID());

			if (textMsg == null)
				textMsg = "";

			textMsg += getActivityTextMsg();

			if (textMsg.length() > 0)
				return HISTORY_DIV_START_TAG + textMsg.replaceAll("\n", "<br />") + "</div>";
			else
				return HISTORY_DIV_START_TAG + "</div>";
		}

		// 3. Special handling for M_Production
		if ("M_Production".equals(tableName)) {
			String sql = "SELECT pp_productioninfo_sp(?)";
			String textMsg = DB.getSQLValueString(null, sql, m_activity.getRecord_ID());
			if (textMsg != null)
				return HISTORY_DIV_START_TAG + textMsg.replaceAll("\n", "<br />") + "</div>";
		}

		// 4. Default History HTML
		return HISTORY_DIV_START_TAG + m_activity.getHistoryHTML() + "</div>";
	}

	/** 取會簽段 HTML（即時呼叫 countersignmessage 函式）；無會簽資料回空字串。 */
	private String getCountersignContent() {
		String html = DB.getSQLValueString(null,
				"SELECT countersignmessage(CAST(? AS numeric), CAST(? AS numeric))",
				m_activity.getAD_Table_ID(), m_activity.getRecord_ID());
		return (html == null || html.trim().length() == 0) ? "" : "<br/>" + html;
	}

	/** 取簽核意見段 HTML（即時呼叫 approvalcommentmessage 函式）；無資料回空字串。 */
	private String getApprovalCommentContent() {
		String html = DB.getSQLValueString(null,
				"SELECT approvalcommentmessage(CAST(? AS numeric), CAST(? AS numeric))",
				m_activity.getAD_Table_ID(), m_activity.getRecord_ID());
		return (html == null || html.trim().length() == 0) ? "" : "<br/>" + html;
	}

	private String getActivityTextMsg() {
		// TODO Auto-generated method stub
		int counter = 0;
		String info = "";
		String whereSQl = "exists (select 1 from ad_wf_node xx where AD_WF_Activity.ad_wf_node_id = xx.ad_wf_node_id and action = 'C' ) "
				+ "and ad_table_id = ? and record_id = ?";
		List<MWFActivity> list = new Query(m_activity.getCtx(), MWFActivity.Table_Name, whereSQl,
				m_activity.get_TrxName())
				.setParameters(new Object[] { m_activity.getAD_Table_ID(), m_activity.getRecord_ID() })
				.setOrderBy(MWFActivity.COLUMNNAME_AD_WF_Activity_ID)
				.list();

		for (MWFActivity activity : list) {
			info += activity.getUpdated().toString().substring(0, 19) + " " + activity.getAD_User().getName() + " Memo:"
					+ activity.getTextMsg() + "\n";
			counter++;
		}
		if (counter > 0)
			info = "\n ==相關已簽核內容==\n" + info;
		return info;
	}

	// ====================================================================
	// 會簽指定（人/角色）
	// ====================================================================

	/**
	 * 此節點同意後是否會走到會簽 node：
	 * 直接出線指向 Value=CounterSign，或經過一個中間節點（如會簽通知 CountersignNotice）再到會簽 node。
	 */
	private boolean leadsToCountersign(MWFNode node) {
		// 會簽 node 本身永不顯示選人框（即使有短迴圈接回，會簽人也不該看到指定欄位）
		if (NODE_VALUE_COUNTERSIGN.equals(node.getValue())) {
			return false;
		}
		String sql = "SELECT COUNT(*) FROM AD_WF_NodeNext nn1 "
				+ "JOIN AD_WF_Node n1 ON n1.AD_WF_Node_ID = nn1.AD_WF_Next_ID "
				+ "WHERE nn1.AD_WF_Node_ID=? AND nn1.IsActive='Y' AND ("
				+ "  n1.Value=? "
				+ "  OR EXISTS (SELECT 1 FROM AD_WF_NodeNext nn2 "
				+ "             JOIN AD_WF_Node n2 ON n2.AD_WF_Node_ID = nn2.AD_WF_Next_ID "
				+ "             WHERE nn2.AD_WF_Node_ID = n1.AD_WF_Node_ID AND nn2.IsActive='Y' AND n2.Value=?))";
		return DB.getSQLValue(null, sql, node.getAD_WF_Node_ID(),
				NODE_VALUE_COUNTERSIGN, NODE_VALUE_COUNTERSIGN) > 0;
	}

	/** 預選上一輪名單（搜尋編輯器用 setValue(逗號分隔 id) 帶入；查無前一輪則清空）。 */
	private void populateCountersignPickers() {
		if (fCsUsers != null)
			fCsUsers.setValue(idsCsv(previousSelection("U", "AD_User_ID")));
		if (fCsRoles != null)
			fCsRoles.setValue(idsCsv(previousSelection("R", "AD_Role_ID")));
	}

	private static String idsCsv(int[] ids) {
		StringBuilder sb = new StringBuilder();
		for (int id : ids) {
			if (sb.length() > 0)
				sb.append(",");
			sb.append(id);
		}
		return sb.length() > 0 ? sb.toString() : null;
	}

	/** 上一輪（最新 process）已指定的對象，供重簽時預選。 */
	private int[] previousSelection(String targetType, String idColumn) {
		String sql = "SELECT DISTINCT " + idColumn + " FROM TG_Countersign"
				+ " WHERE AD_Table_ID=? AND Record_ID=? AND TargetType=? AND " + idColumn + ">0"
				+ " AND AD_WF_Process_ID=(SELECT MAX(AD_WF_Process_ID) FROM TG_Countersign"
				+ " WHERE AD_Table_ID=? AND Record_ID=?)";
		return DB.getIDsEx(null, sql, m_activity.getAD_Table_ID(), m_activity.getRecord_ID(), targetType,
				m_activity.getAD_Table_ID(), m_activity.getRecord_ID());
	}

	/** 將勾選的人/角色寫成 TG_Countersign 待簽列（GenericPO；CreatedBy 由 PO 自動帶＝目前簽核者）。 */
	private void writeCountersign(String trxName) {
		int tableId = m_activity.getAD_Table_ID();
		int recordId = m_activity.getRecord_ID();
		int processId = m_activity.getAD_WF_Process_ID();
		int orgId = m_activity.getAD_Org_ID();
		if (fCsUsers != null)
			for (ValueNamePair vnp : fCsUsers.getComponent().getChosenbox().getSelectedObjects())
				insertCountersign(tableId, recordId, processId, orgId, "U", Integer.parseInt(vnp.getValue()), 0, trxName);
		if (fCsRoles != null)
			for (ValueNamePair vnp : fCsRoles.getComponent().getChosenbox().getSelectedObjects())
				insertCountersign(tableId, recordId, processId, orgId, "R", 0, Integer.parseInt(vnp.getValue()), trxName);
	}

	private void insertCountersign(int tableId, int recordId, int processId, int orgId,
			String targetType, int userId, int roleId, String trxName) {
		PO cs = MTable.get(Env.getCtx(), "TG_Countersign").getPO(0, trxName);
		cs.set_ValueOfColumn("AD_Org_ID", orgId);
		cs.set_ValueOfColumn("AD_Table_ID", tableId);
		cs.set_ValueOfColumn("Record_ID", recordId);
		cs.set_ValueOfColumn("AD_WF_Process_ID", processId);
		cs.set_ValueOfColumn("TargetType", targetType);
		if (userId > 0)
			cs.set_ValueOfColumn("AD_User_ID", userId);
		if (roleId > 0)
			cs.set_ValueOfColumn("AD_Role_ID", roleId);
		cs.set_ValueOfColumn("Status", "W");
		cs.saveEx();
	}

	// ====================================================================
	// 拆單（依 SysConfig 對應 Info Window）
	// ====================================================================

	/**
	 * 由 SysConfig {@code TABLE_SUPPORT_SPLIT}（格式 {@code TableName:AD_InfoWindow_ID}，逗號分隔）
	 * 取指定單據對應的拆單 Info Window ID；查無對應或設定空字串時回 0（＝不顯示拆單）。
	 */
	private int getSplitInfoWindowID(int tableId) {
		String csv = MSysConfig.getValue("TABLE_SUPPORT_SPLIT", "");
		if (csv == null || csv.trim().length() == 0)
			return 0;
		for (String pair : csv.split(",")) {
			int sep = pair.lastIndexOf(":");
			if (sep <= 0)
				continue;
			String tableName = pair.substring(0, sep).trim();
			if (MTable.getTable_ID(tableName) != tableId)
				continue;
			try {
				return Integer.parseInt(pair.substring(sep + 1).trim());
			} catch (NumberFormatException e) {
				log.warning("TABLE_SUPPORT_SPLIT 設定的 InfoWindow ID 非數字: " + pair);
				return 0;
			}
		}
		return 0;
	}

	/**
	 * 拆單：以當前單據開對應的 Info Window，並把單據 context 以 predefined 變數帶入，
	 * 供 Info Window 的 WHERE（{@code @+Split_Record_ID@}）與後續拆單 Process 使用。
	 */
	private void cmd_split() {
		if (m_activity == null || m_splitInfoWindowID <= 0)
			return;
		// 換行分隔的 var=value；Info Window 內以 @+var@ 取用（前綴 + 為 predefined 變數）
		String vars = "Split_Record_ID=" + m_activity.getRecord_ID()
				+ "\nSplit_AD_Table_ID=" + m_activity.getAD_Table_ID()
				+ "\nSplit_AD_WF_Activity_ID=" + m_activity.getAD_WF_Activity_ID();
		// 自建 Info Window（帶 predefined 變數）後以視窗模式顯示，並立即查詢，免使用者手動重新整理
		InfoWindow iw = InfoManager.create(m_splitInfoWindowID, vars);
		if (iw == null) {
			FDialog.error(m_WindowNo, this, "NotValid");
			return;
		}
		iw.setAttribute(Window.MODE_KEY, Window.Mode.OVERLAPPED);
		iw.setCloseAfterExecutionOfProcess(true); // 拆單 Process 跑完自動關閉
		iw.setClosable(true);
		iw.setSizable(true);
		iw.setMaximizable(true);
		iw.setBorder("normal");
		iw.setContentStyle("overflow: auto");
		ZKUpdateUtil.setWidth(iw, "85%");
		ZKUpdateUtil.setHeight(iw, "85%");
		// Info Window 關閉時：若不是取消（代表跑過拆單 Process），重載簽核清單，
		// 避免已處理的活動仍留在畫面被誤按第二次
		iw.addEventListener(DialogEvents.ON_WINDOW_CLOSE, e -> {
			if (!iw.isCancelled()) {
				loadActivities();
				display(-1);
			}
		});
		AEnv.showWindow(iw);
		iw.onUserQuery(); // 開啟即執行查詢（@+Split_Record_ID@ 已就緒）
	} // cmd_split

	/**
	 * 唯讀檢視當前單據附件：列出附件、每筆可檢視/下載；不提供新增/刪除。
	 */
	private void cmd_attachment() {
		if (m_activity == null)
			return;
		MAttachment att = MAttachment.get(Env.getCtx(), m_activity.getAD_Table_ID(), m_activity.getRecord_ID());
		if (att == null || att.getEntryCount() == 0) {
			Clients.showNotification("無附件");
			return;
		}

		Window win = new Window();
		win.setTitle("附件檢視（唯讀）");
		win.setClosable(true);
		win.setSizable(true);
		win.setMaximizable(true);
		win.setBorder("normal");
		win.setContentStyle("overflow:auto");
		ZKUpdateUtil.setWidth(win, "480px");
		win.setAttribute(Window.MODE_KEY, Window.Mode.OVERLAPPED);

		Vlayout vlist = new Vlayout();
		vlist.setStyle("padding:10px;");
		ZKUpdateUtil.setHflex(vlist, "1");
		for (int i = 0; i < att.getEntryCount(); i++) {
			final MAttachmentEntry entry = att.getEntry(i);
			Hbox row = new Hbox();
			row.setAlign("center");
			ZKUpdateUtil.setHflex(row, "1");
			Label name = new Label((i + 1) + ". " + entry.getName());
			ZKUpdateUtil.setHflex(name, "1");
			Button btnView = new Button("檢視");
			btnView.addEventListener(Events.ON_CLICK, e -> previewEntry(entry));
			Button btnDl = new Button("下載");
			btnDl.addEventListener(Events.ON_CLICK, e -> Filedownload
					.save(new AMedia(entry.getName(), null, entry.getContentType(), entry.getData())));
			row.appendChild(name);
			row.appendChild(btnView);
			row.appendChild(btnDl);
			vlist.appendChild(row);
		}
		win.appendChild(vlist);
		AEnv.showWindow(win);
	} // cmd_attachment

	/** 內嵌預覽單一附件：以 Iframe 直接 render（PDF/圖片/文字等瀏覽器可內看的型別），不下載。 */
	private void previewEntry(MAttachmentEntry entry) {
		Window win = new Window();
		win.setTitle(entry.getName());
		win.setClosable(true);
		win.setSizable(true);
		win.setMaximizable(true);
		win.setBorder("normal");
		win.setContentStyle("overflow:hidden");
		ZKUpdateUtil.setWidth(win, "80%");
		ZKUpdateUtil.setHeight(win, "85%");
		win.setAttribute(Window.MODE_KEY, Window.Mode.OVERLAPPED);

		Iframe iframe = new Iframe();
		ZKUpdateUtil.setWidth(iframe, "100%");
		ZKUpdateUtil.setHeight(iframe, "100%");
		iframe.setContent(new AMedia(entry.getName(), null, entry.getContentType(), entry.getData()));
		iframe.setClientAttribute("sandbox", ""); // 沙箱化，避免附件內嵌腳本
		win.appendChild(iframe);
		AEnv.showWindow(win);
	} // previewEntry

	/**
	 * Zoom
	 */
	private void cmd_zoom() {
		if (log.isLoggable(Level.CONFIG))
			log.config("Activity=" + m_activity);
		if (m_activity == null)
			return;
		AEnv.zoom(m_activity.getAD_Table_ID(), m_activity.getRecord_ID());
	} // cmd_zoom

	/**
	 * Answer Button
	 */
	private void cmd_button() {
		if (log.isLoggable(Level.CONFIG))
			log.config("Activity=" + m_activity);
		if (m_activity == null)
			return;
		//
		MWFNode node = m_activity.getNode();
		if (MWFNode.ACTION_UserWindow.equals(node.getAction())) {
			int AD_Window_ID = node.getAD_Window_ID(); // Explicit Window
			String ColumnName = m_activity.getPO().get_TableName() + "_ID";
			int Record_ID = m_activity.getRecord_ID();
			MQuery query = MQuery.getEqualQuery(ColumnName, Record_ID);
			boolean IsSOTrx = m_activity.isSOTrx();
			//
			log.info("Zoom to AD_Window_ID=" + AD_Window_ID
					+ " - " + query + " (IsSOTrx=" + IsSOTrx + ")");

			AEnv.zoom(AD_Window_ID, query);
		} else if (MWFNode.ACTION_UserForm.equals(node.getAction())) {
			int AD_Form_ID = node.getAD_Form_ID();

			ADForm form = ADForm.openForm(AD_Form_ID);
			form.setAttribute(Window.MODE_KEY, form.getWindowMode());
			AEnv.showWindow(form);
		} else if (MWFNode.ACTION_UserInfo.equals(node.getAction())) {
			SessionManager.getAppDesktop().openInfo(node.getAD_InfoWindow_ID());
		} else
			log.log(Level.SEVERE, "No User Action:" + node.getAction());
	} // cmd_button

	/**
	 * Save
	 */
	public void onOK() {
		if (log.isLoggable(Level.CONFIG))
			log.config("Activity=" + m_activity);
		if (m_activity == null) {
			Clients.clearBusy();
			return;
		}
		int AD_User_ID = Env.getAD_User_ID(Env.getCtx());
		String textMsg = fTextMsg.getValue();
		// 代簽留痕：請假主管的待辦由代理人(登入者)簽核時自動標註
		textMsg = applyDelegateTag(m_activity, textMsg);
		//
		MWFNode node = m_activity.getNode();

		Object forward = fForward.getValue();

		// ensure activity is ran within a transaction - [ 1953628 ]
		Trx trx = null;
		try {
			/**
			 * 2020-10-29 fixed
			 */
			// trx = Trx.get(Trx.createTrxName("FWFA"), true);

			// m_activity.set_TrxName(trx.getTrxName());
			trx = Trx.get(Trx.createTrxName(m_activity.get_TrxName()), true);
			trx.setDisplayName(getClass().getName() + "_onOK");
			if (forward != null) {
				if (log.isLoggable(Level.CONFIG))
					log.config("Forward to " + forward);
				int fw = ((Integer) forward).intValue();
				if (fw == AD_User_ID || fw == 0) {
					log.log(Level.SEVERE, "Forward User=" + fw);
					trx.rollback();
					trx.close();
					return;
				}
				if (!m_activity.forwardTo(fw, textMsg)) {
					FDialog.error(m_WindowNo, this, "CannotForward");
					trx.rollback();
					trx.close();
					return;
				}
			}
			// User Choice - Answer
			else if (MWFNode.ACTION_UserChoice.equals(node.getAction())) {
				if (m_column == null)
					m_column = node.getColumn();
				// Do we have an answer?
				int dt = m_column.getAD_Reference_ID();
				String value = fAnswerText.getText();
				if (dt == DisplayType.YesNo || dt == DisplayType.List) {
					ListItem li = fAnswerList.getSelectedItem();
					if (li != null)
						value = li.getValue().toString();
				}
				if (value == null || value.length() == 0) {
					FDialog.error(m_WindowNo, this, "FillMandatory", Msg.getMsg(Env.getCtx(), "Answer"));
					trx.rollback();
					trx.close();
					return;
				}
				//
				if (log.isLoggable(Level.CONFIG))
					log.config("Answer=" + value + " - " + textMsg);
				try {
					// 同意(Y)且此節點接會簽 → 先在同一 trx 寫會簽名單，
					// setUserChoice 推進進會簽 node 後，fan-out 才撈得到本輪名單。
					if (leadsToCountersign(node) && "Y".equals(value)) {
						m_activity.set_TrxName(trx.getTrxName());
						writeCountersign(trx.getTrxName());
					}
					m_activity.setUserChoice(AD_User_ID, value, dt, textMsg);
				} catch (Exception e) {
					log.log(Level.SEVERE, node.getName(), e);
					FDialog.error(m_WindowNo, this, "Error", e.toString());
					trx.rollback();
					trx.close();
					return;
				}
			}
			// User Action
			else {
				if (log.isLoggable(Level.CONFIG))
					log.config("Action=" + node.getAction() + " - " + textMsg);
				try {
					// ensure activity is ran within a transaction
					m_activity.setUserConfirmation(AD_User_ID, textMsg);
				} catch (Exception e) {
					log.log(Level.SEVERE, node.getName(), e);
					FDialog.error(m_WindowNo, this, "Error", e.toString());
					trx.rollback();
					trx.close();
					return;
				}

			}

			trx.commit();
		} finally {
			Clients.clearBusy();
			if (trx != null)
				trx.close();
		}

		// Next
		loadActivities();
		display(-1);
	} // onOK

	/**
	 * Batch Approve
	 */
	private void cmd_batchApprove() {
		int AD_User_ID = Env.getAD_User_ID(Env.getCtx());
		String textMsg = "Batch Approve";

		int count = 0;
		if (listbox.getSelectedCount() == 0)
			return;

		for (Listitem item : listbox.getSelectedItems()) {
			int index = item.getIndex();
			if (index < 0 || index >= m_activities.length)
				continue;

			MWFActivity activity = m_activities[index];
			MWFNode node = activity.getNode();

			Trx trx = null;
			try {
				trx = Trx.get(Trx.createTrxName(activity.get_TrxName()), true);
				trx.setDisplayName(getClass().getName() + "_batch");

				String msg = applyDelegateTag(activity, textMsg);
				if (MWFNode.ACTION_UserChoice.equals(node.getAction())) {
					MColumn column = node.getColumn();
					int dt = column.getAD_Reference_ID();
					String value = "Y";
					activity.setUserChoice(AD_User_ID, value, dt, msg);
				} else {
					activity.setUserConfirmation(AD_User_ID, msg);
				}

				trx.commit();
				count++;
			} catch (Exception e) {
				if (trx != null)
					trx.rollback();
				log.log(Level.SEVERE, "Batch Error: " + activity, e);
			} finally {
				if (trx != null)
					trx.close();
			}
		}

		Clients.showNotification("Batch Approved: " + count);
		loadActivities();
		display(-1);
	}

}
