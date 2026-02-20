#include <std.h>
#include <quickdraw.h>
#include <dialog.h>
#include <event.h>
#include <control.h>
#include "vars.h"
#include "pharaoh.h"
#include "strings.h"

/*«DoSpread»
  * DoSpread -- Tell the system how many tons of manure to spread
  */
  
VOID DoSpread()
	{
	DialogRecord d;
	BITS item;
	Rect r;
	
	GetNewDialog(D_SPREAD, &d, -1L);

	FOREVER
		{
		ModalDialog(NIL, &item);
		
		if (item == DI_CANCEL)
			break;
		else if (item == DI_OK)
			{
			TEXT num[256]; /* dialog edits can't return anything bigger */
			BITS x;
			Handle editHandle;
			DOUBLE atof(), newMnToSprd;
			
			GetDItem(&d, DISP_EDIT, &x, &editHandle, &r);
			GetIText(editHandle, &num);
			ptoc(&num);
			
			if (!IsNumeric(&num))
				{
				ErrorStr(ST_SPIN);
				continue;
				}
				
			newMnToSprd = atof(&num);
			
			/* now do a little error checking */
			if (newMnToSprd < 0)
				{
				ErrorStr(ST_SPNN);
				SelIText(&d, DISP_EDIT, 0, 32767);
				continue;
				}
			
			mnToSprd = newMnToSprd;
			break;
			}
		}
	CloseDialog(&d);
	}
