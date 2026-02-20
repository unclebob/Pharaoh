/*
 * skel.h -- The Skelaton Header file. 
 */
 
/*«Window Refcons»
 * Window Refcons are pointers to window Control Data structures
 */
 
typedef struct
	{
	LONG windowID;
	VOID (*windowCursor)();		/* Function figures out what the window's cursor should be */
	VOID (*windowIdle)();		/* Function performs the Idling for the window */
	VOID (*windowMouse)();		/* Function handles all mouse events for the window */
	VOID (*windowUpdate)();		/* Function handles all update events for the window */
	VOID (*windowKey)();		/* Function handles all key events for the window */
	VOID (*windowActivate)();	/* Function handles all Activate events for the window */
	BOOL (*windowCommand)();	/* Function handles menu commands pertinent to the window */
	VOID (*windowFont)();		/* Function sets font and style for the window */
	Handle windowPile;		/* Handle to data storage area for the window */
	} WindowControl;
	
/*    window control routine calling formats
 *
 * windowCursor(WindowPtr, Point, code);
 * windowIdle(WindowPtr);
 * windowMouse(WindowPtr, EventRecord, code);
 * windowUpdate(WindowPtr, EventRecord);
 * windowKey(WindowPtr, EventRecord);
 * windowActivate(WindowPtr, EventRecord);
 * windowCommand(WindowPtr, command);   //returns YES if command processed//
 * windowFont(WindowPtr, font, size, style);
 */
 
/*«Menu Stuff» Apple,  Font and Style menus */

#define APPLE_MENU	1	/* menu ID of the APPLE menu */
#define FONT_MENU		100	/* menu ID of the FONT menu */
#define STYLE_MENU	101	/* menu ID of the STYLE menu */ 

/* standard menu commands */

#define C_NULL		-1	/* un-translatable menu item */

/* standard edit commands */

#define C_UNDO		900	/* undo last change */
#define C_CUT			902	/* cut selection */
#define C_COPY		903	/* copy selection */
#define C_PASTE		904	/* paste selection */
#define C_CLEAR		905	/* clear selection */

#define MENU_START 128	/* resource ID of the initial menu bar */

/*«Standard Event Junk»
 * Event stuff
 */

#define TRANSITION		app1Evt	/* A transition event.   eventRecord.message contains
							      The transition type */

#define TR_DESKACC	500	/* desk accessory is front window.*/
#define TR_NULL		501	/* Do not change state, but do cause FSM to look at itself, and
						     restore its normal state.   Used to restore things after the use
						     of a desk accessory */
							      

