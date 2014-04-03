package ai.aitia.testing.fest;

public class CompNames {
	
	public class Btn
	{
		static final String OK = "btn_ok";
		static final String HELP = "btn_help";
		static final String CANCEL = "btn_cancel";
		static final String YES = "btn_yes";
		static final String NO = "btn_no";
		static final String BACK = "btn_back";
		static final String NEXT = "btn_next";
		static final String FINISH = "btn_finish";
		static final String CMENUCUT = "btn_cut";
		static final String CMENUPASTE = "btn_paste";
		static final String CMENUSELECTALL = "btn_selectall";
		
		public class Main
		{
			//főablak toolbar gombok
			static final String IMPORT = "btn_mainwindow_import";
			static final String EXPORT = "btn_mainwindow_export";
			static final String RENAME = "btn_mainwindow_renamemodel";
			static final String DELETE = "btn_mainwindow_deletemodel";
			static final String CREATEVIEW = "btn_mainwindow_createview";
			static final String LOAD = "btn_mainwindow_loadview";
			static final String CREATECHART = "btn_mainwindow_createchart";
			static final String RUN = "btn_mainwindow_runsimulation";
			static final String MONITOR = "btn_mainwindow_monitorsimulation";
			static final String DOWNLOAD = "btn_mainwindow_clouddownload";
			
			//resultlist cmenü gombok
			static final String CMENUEXPORT = "btn_mainwindow_treecmenuexport";
			static final String CMENUIMPORT = "btn_mainwindow_treecmenuimport";
			static final String CMENURENAME = "btn_mainwindow_treecmenurename";
			static final String CMENUDELETE = "btn_mainwindow_treecmenudelete";
			static final String CMENUCREATEVIEW = "btn_mainwindow_treecmenucreateview";
			static final String CMENULOAD = "btn_mainwindow_treecmenuimportloadview";
			static final String CMENUCREATE = "btn_mainwindow_treecmenuimportcreatechart";
		
		}

		public class Platforms
		{
			static final String REGISTER = "btn_platforms_register";
			
		}
		
		public class Models
		{
			static final String DIRBROWSE = "btn_wizard_modelselection_dirbrowse";
			static final String BROWSE = "btn_wizard_modelselection_browse";
			static final String PARAMBROWSE = "btn_wizard_modelselection_parambrowse";
			static final String ADD  = "btn_wizard_modelselection_add";
			static final String REMOVE = "btn_wizard_modelselection_remove";
			static final String UP = "btn_wizard_modelselection_up";
			static final String DOWN = "btn_wizard_modelselection_down";
			static final String TOP = "btn_wizard_modelselection_top";
			static final String BOTTOM = "btn_wizard_modelselection_bottom";
			static final String LOAD = "btn_wizard_modelselection_load";
			static final String RESOURCES = "btn_wizard_modelselection_resources";
			static final String PREFERENCES = "btn_wizard_modelselection_preferences";
			static final String ABOUT = "btn_wizard_modelselection_about";
		}
		
		public class Parameters
		{
			static final String MODIFY = "btn_wizard_params_modify";
			static final String CANCEL = "btn_wizard_params_cancel";
			static final String CMENUEDIT = "btn_wizard_params_cmenuedit";
			static final String CMENUMOVEUP = "btn_wizard_params_cmenumoveup";
			static final String CMENUMOVEDOWN = "btn_wizard_params_cmenumovedown";
			static final String DOWN = "btn_wizard_params_movedown";
			static final String UP = "btn_wizard_params_moveup";
			static final String EDIT = "btn_wizard_params_edit";
		}
		
		public class Recorders
		{
			static final String STOPEXTENDED = "btn_wizard_record_stopextended";
			static final String WHENEXTENDED = "btn_wizard_record_whenextended";
			static final String BROWSEOUTPUT = "btn_wizard_record_browseoutput";
			static final String ADVANCED  = "btn_wizard_record_advanced";
			static final String ADDRECORDER = "btn_wizard_record_addrecorder";
			static final String CANCEL = "btn_wizard_record_cancel";
			static final String ADD = "btn_wizard_record_add";
			static final String ADDAS = "btn_wizard_record_addas";
			static final String NEWRECORDER = "btn_wizard_record_create";
			static final String EDITRECORDER = "btn_wizard_record_edit";
			static final String REMOVERECORDER = "btn_wizard_record_remove";
			static final String CMENUNEW = "btn_newrecorder";
			static final String CMENUEDIT = "btn_editrecorder";
			static final String CMENUREMOVE = "btn_removerecorder";
			static final String CREATE = "btn_wizard_record_createscript";
			static final String REMOVE = "btn_wizard_record_removescript";
			static final String EDIT = "btn_wizard_record_editscript";
			static final String TEST = "btn_wizard_record_testscript";
			
			
		}
		
		public class RepastImport
		{
			static final String INPUT = "btn_repast_input";
			static final String OUTPUT = "btn_repast_output";
		}
		
		public class DBChooser
		{
			static final String BROWSE = "btn_dbchooser_browse";
		}
		
		public class CSVImport
		{
			static final String SCANCEL = "btn_csvimport_scancel";
			static final String FCANCEL = "btn_csvimport_fcancel";
			
		}
		public class DataSource
		{
			/**note: append gui number*/
			static final String CONST = "btn_datasource_const";
			static final String ADD = "btn_datasource_add";
			static final String REMOVE = "btn_datasource_remove";
		}
		
		public class Factorial
		{
			static final String ADD = "btn_factorial_add";
			static final String REMOVE = "btn_factorial_remove";
			static final String SETDEF = "btn_factorial_setdefault";
			static final String SETLEVEL = "btn_factorial_setlevel";
			static final String SETCOL = "btn_factorial_setcolumn";
			static final String UP = "btn_factorial_moveup";
			static final String DOWN = "btn_factorial_movedown";
		}
		public class Preferences
		{
			static final String REGISTER = "btn_preferences_register";
			static final String DEREGISTER = "btn_preferences_deregister";
			static final String BROWSE = "btn_preferences_browse";
			
		}
		
	}
	
	public class Fld
	{
		
		static final String CLASSNOTFOUND = "fld_classpath";
		
		public class EEditor
		{
			static final String TEXT = "fld_eeditor";
		}
		
		public class Models
		{
			static final String MODELDIR = "btn_repast_input";
			static final String MODELFILE = "btn_repast_input";
		}
		
		public class Parameters
		{
			static final String RUNS = "fld_wizard_params_runs";
			static final String START = "fld_wizard_params_incrstart";
			static final String END = "fld_wizard_params_incrend";
			static final String STEP = "fld_wizard_params_incrstep";
			static final String CONST = "fld_wizard_params_constval";
			static final String LIST = "fld_wizard_params_paramlist";
		}
		
		public class Recorders
		{
			static final String STOP = "fld_wizard_record_stopfld";
			static final String NAME = "fld_wizard_record_namefld";
			static final String OUTPUT = "fld_wizard_record_output";
			static final String AFTER = "fld_wizard_record_afteriteration";
			static final String CONDITION = "fld_wizard_record_whencnd";
		}
		
		public class RepastImport
		{
			static final String FILENAME = "fld_repast_filename";
			static final String MODELNAME = "fld_repast_modelname";
			static final String VERSION = "fld_repast_version";
			static final String BATCHNAME = "fld_repast_batch";
			static final String INTELLISWEEP = "fld_repast_intellisweep";
		}
		
		public class NLogoImport
		{
			static final String FILENAME = "fld_nlogo_filename";
			static final String MODELNAME = "fld_nlogo_modelname";
			static final String VERSION = "fld_nlogo_version";
			static final String BATCHNAME = "fld_nlogo_batch";
			static final String INTELLISWEEP = "fld_nlogo_intellisweep";
		}
		
		public class DBChooser
		{
			static final String LOGIN = "fld_dbchooser_login";
			static final String PASSWORD = "fld_dbchooser_password";
			static final String EXTDBNAME = "fld_dbchooser_extdb";
			static final String INTDBNAME = "fld_dbchooser_intdb";	
		}
		
		public class WriteToFile
		{
			static final String TICK = "fld_wtfdialog_tick";
		}
		
		public class DataSource
		{
			/**note: append gui number*/
			static final String CONST = "fld_datasource_const";
			static final String NAME = "fld_datasource_name";
		}
		
		public class Factorial
		{
			static final String PSPINNER = "fld_factorial_pspinner";
			static final String CENTERSPINNER = "fld_factorial_centerspinner";
			static final String LOW = "fld_factorial_low";
			static final String HIGH = "fld_factorial_high";
			static final String DEFAULT = "fld_factorial_default";
		}
		public class Preferences
		{
			static final String PLATFORMDIR = "fld_preferences_platformdir";
		}
	}
	public class RBtn
	{
		public class Parameters
		{
			static final String CONST = "rbtn_wizard_params_const";
			static final String LIST = "rbtn_wizard_params_list";
			static final String INCREMENT = "rbtn_wizard_params_incr";
		}
		
		public class Recorders
		{
			static final String STOPITERATION = "rbtn_wizard_record_stopiteration";
			static final String STOPCONDITION = "rbtn_wizard_record_stopcondition";
			static final String ENDITERATION = "rbtn_wizard_record_enditeration";
			static final String AFTERITERATION = "rbtn_wizard_record_afteriteration";
			static final String ENDRUN = "rbtn_wizard_record_endrun";
			static final String WHENCONDITION = "rbtn_wizard_record_whencnd";
		}
		
		public class DBChooser
		{
			public final static String INTERNAL = "rbtn_dbchooser_internal";
			public final static String EXTERNAL = "rbtn_dbchooser_external";
		}
		
		public class WriteToFile
		{
			static final String EVERYRECORDING = "rbtn_wtfdialog_record";
			static final String ENDOFRUNS = "rbtn_wtfdialog_run";
			static final String AFTER = "rbtn_wtfdialog_tick";
		}
		public class Factorial
		{
			static final String FACTDIFF = "rbtn_factorial_difference";
			static final String FACTCOEFF = "rbtn_factorial_coeffs";
		}
	}
	
	public class Dial
	{
		static final String POPUP = "popupdialog";
		static final String CLASSPATH = "dial_classpath";
		static final String WIZARD = "dial_wizard";
		static final String REPASTIMPORT = "dial_repastimport";
		static final String NLOGOIMPORT = "dial_nlogoimport";
		static final String CSVIMPORT = "dial_csvimport";
		static final String EEDITOR = "dial_eeditor";
		static final String WTF = "dial_writetofile";
		static final String DATASOURCE = "dial_datasource";
		static final String INTELLIPROCESS = "dial_intelliprocess";
		static final String PREFERENCES = "dial_preferences";
	}

	public class Tree
	{
		public class Main
		{
			static final String RESULTTREE = "tree_mainwindow_resulttree";
		}
		
		public class Parameters
		{
			static final String PARAMTREE = "tree_wizard_params";
		}
		
		public class Recorders
		{
			static final String RECORDERTREE = "tree_wizard_record_recordertree";
		}
		
		public class DataSource
		{	/**note: append gui number*/
			static final String PARAMTREE = "tree_datasource_paramtree";
		}
		public class Preferences
		{
			static final String PREFTREE = "tree_preferences_preftree";
		}
	}
	
	public class CMenu
	{
		public class Main
		{
			static final String RESULTSMENU = "cmenu_mainwindow_treecmenu";
		}
		
		public class Prameters
		{
			static final String PARAMSMENU = "cmenu_wizard_parameters_treecmenu";
		}
		
		public class Recorders
		{
			static final String RECORDERSMENU = "cmenu_wizard_record_recorderscmenu";
			static final String RECORDABLESMENU = "cmenu_wizard_record_recordablescmenu";
		}
	}
	
	public class Lst
	{
		public class Platforms
		{
			static final String PLATFORMLIST = "lst_wizard_platformlst";
		}
		
		public class Recorders
		{
			static final String VARIABLES = "lst_wizard_record_varlst";
			static final String METHODS = "lst_wizard_record_methodlst";
			static final String SCRIPTS = "lst_wizard_record_datasrclst";
			static final String MISC = "lst_wizard_record_misclst";
			
		}
		public class Methods
		{
			static final String METHOD = "lst_wizard_methodselection_methodlst";
		}
		public class Models
		{
			static final String CLASSPATH = "lst_wizard_modelselection_classpath";

		}
		public class DataSource
		{
			static final String STATS = "lst_datasource_availstats";
			static final String OPS = "lst_datasource_availops";
			/**note: append gui number*/
			static final String SELECTED = "lst_datasource_selected";
		}
		
		public class Factorial
		{
			static final String PARAMS = "lst_factorial_paramlist";
			static final String FACTORS = "lst_factorial_factors";
		}
		
	}
	
	public class Menu
	{
		public class Main
		{
			/*static final String MENUBAR = "menubar_mainwindow";
			static final String FILE = "menu_mainwindow_file";
			static final String VIEWS = "menu_mainwindow_views";
			static final String CHARTS = "menu_mainwindow_charts";
			static final String PARAMSWEEP = "menu_mainwindow_paramsweep";
			static final String WINDOW = "menu_mainwindow_window";
			static final String TOOLS = "menu_mainwindow_tools";
			static final String HELP = "menu_mainwindow_help";*/
		
			public class Import
			{
				static final String CSV = "menuitem_file_import_csvfile";
				static final String NETLOGO = "menuitem_file_import_netlogoresultfile";
				static final String REPAST = "menuitem_file_import_repastjresultfile";
				static final String SIMPHONY = "menuitem_file_import_repastsimphonyresultfile";
				static final String CUSTOM = "menuitem_file_import_customjavamodelresultfile";
				static final String MASON = "menuitem_file_import_masonmodelresultfile";
				static final String TEST = "menuitem_file_import_testimportplugin";
			}
			
			public class Export
			{
				static final String CSV = "menuitem_file_export_csvfile";
				static final String TEST = "menuitem_file_export_testexportplugin";
			}
			
			public class File
			{
				/*static final String	IMPORT = "menuitem_file_import";
				static final String EXPORT = "menuitem_file_export";*/
				static final String DATABASE = "menuitem_file_database";
				static final String LOAD = "menuitem_file_load";
				static final String SAVE = "menuitem_file_save";
				static final String EXIT = "menuitem_file_exit";
				
				
			}
			
			public class Views
			{
				static final String CREATE = "menuitem_views_create";
				static final String	LOAD = "menuitem_views_load";
				static final String SAVE = "menuitem_views_save";
				static final String RECREATE = "menuitem_views_recreate";
				static final String RENAME = "menuitem_views_rename";
				static final String DELETE = "menuitem_views_delete";
			}
			
			public class Charts
			{
				static final String CREATE = "menuitem_charts_create";
				static final String OPEN = "menuitem_charts_open";
				static final String EXPORT = "menuitem_charts_export";
			}
			
			public class ParamSweep
			{
				static final String	RUN = "menuitem_paramsweep_run";
				static final String MONITORSIM = "menuitem_paramsweep_monitorsim";
				static final String WIZARD = "menuitem_paramsweep_wizard";
				static final String MONITORPREF = "menuitem_paramsweep_monitorprefs";
				static final String DOWNLOAD = "menuitem_paramsweep_download";
			}
			
			public class Help
			{
				static final String TUTORIAL = "menuitem_help_tutorial";
				static final String MANUAL = "menuitem_help_manual";
				static final String LOG = "menuitem_help_log";
				static final String ABOUT = "menuitem_help_about";
			}
			
			public class Window
			{
				/*static final String LAYOUT = "menuitem_window_layout";
				static final String SKIN = "menuitem_window_skin";
				static final String THEME = "menuitem_window_theme";
				static final String WATERMARK = "menuitem_window_watermark";*/
				
				public class Skin
				{
					/**note: append button number*/
					static final String BUTTON1 = "menuitem_window_skin_button";
				}
				
				public class Theme
				{
					static final String BUTTON1 = "menuitem_window_theme_button1";
					static final String BUTTON2 = "menuitem_window_theme_button2";
					static final String BUTTON3 = "menuitem_window_theme_button3";
					static final String BUTTON4 = "menuitem_window_theme_button4";
					static final String BUTTON5 = "menuitem_window_theme_button5";
					static final String BUTTON6 = "menuitem_window_theme_button6";
					static final String BUTTON7 = "menuitem_window_theme_button7";
					static final String BUTTON8 = "menuitem_window_theme_button8";
					static final String BUTTON9 = "menuitem_window_theme_button9";
					static final String BUTTON10 = "menuitem_window_theme_button10";
					static final String BUTTON11 = "menuitem_window_theme_button11";
					static final String BUTTON12 = "menuitem_window_theme_button12";
					static final String BUTTON13 = "menuitem_window_theme_button13";
					static final String BUTTON14 = "menuitem_window_theme_button14";
					static final String BUTTON15 = "menuitem_window_theme_button15";
				}
				
				public class Watermark
				{
					/**note: append button number*/
					static final String BUTTON = "menuitem_window_watermark_button";
				}
			}
			
			public class Tools
			{
				static final String CONFIGURE = "menuitem_tools_database";
				static final String	DEFINE = "menuitem_tools_import";
				static final String CSVEXPORT = "menuitem_tools_export";
				/**type: JMenu*/
				/*static final String GROUPS = "menuitem_tools_load";*/
				/**type: JCheckBoxmenuitem_tools*/
				static final String VERBOSE = "menuitem_tools_save";
				/**note: append tool number*/
				static final String USERTOOL = "menuitem_tools_usertool_button";
				
				public class Groups
				{
					/**note: append group number*/
					static final String GROUP = "menuitem_tools_group_button";
				}
			}
		}
		
		
		
	}
	
	public class CBox
	{
		public class RepastImport
		{
			static final String INTELLISWEEP = "cbox_repast_intellicbox";
		}
		
		public class DataSource
		{
			/**note: append label text*/
			static final String NAME = "cbox_datasource_";
		}
		public class Factorial
		{
			static final String CENTERPOINT = "cbox_factorial_centerpoint";
			static final String FRACTIONAL = "cbox_factorial_fractional";
		}
	}
	
	public class Pane
	{
		public class Recorders
		{
			static final String RECORDABLES = "pane_wizard_record_recordables";
		}
		public class DataSource
		{
			static final String GENERATEDGUI = "pane_datasource_generatedgui";
		}
		public class IntelliProcess
		{
			static final String TABPANE = "pane_intelliprocess_tabpane";			
		}
	}
	
	public class FileChooser
	{
		static final String PARAMSWEEP = "filechooser_paramsweep";
		static final String DBCHOOSER = "filechooser_dbchooser";
		static final String CSVCHOOSER = "filechooser_csvimport";
		static final String CLASSPATH = "filechooser_classpath";
	}
	
}
