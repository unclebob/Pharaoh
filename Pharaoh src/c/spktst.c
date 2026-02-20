#include <std.h>
#include <event.h>
#include <resource.h>
#include <window.h>
#include <dialog.h>
#include <segment.h>
#include "macintalk.h"

/*«main»
  * The spktst program begins here
  */

GLOBAL COUNT testEvent = -1;

SpeechHandle theSpeech;

main(ac,av)
COUNT ac;
TEXT **av;
	{
	COUNT **handle;
	TEXT s[256];
	COUNT i, n, ind;
	
	if (ac != 2)
		{
		printf("Usage: spktst strtRes\n");
		exit();
		}
	
	OpenResFile("\Ppharaoh:res:pharaoh.res"); /* open the resources*/
	if (SpeechOn("", &theSpeech) != 0) 
		{
		printf("can't open Macintalk\n");
		exit();
		}

	Say("Now test the speach in the Pharaoh game.");
	
	for (i=atoi(av[1]); i<30000; i++)
		{
		handle = GetResource('STR#', i);
		if (handle == NIL)
			continue;
			
		n = **handle;
		
		printf("STR# %d has %d strings\n", i, n);
		for (ind=1; ind<=n; ind++)
			{
			GetIndString(&s, i, ind);
			ptoc(s);
			printf("(%d,%d): %s\n", i,ind,s);
			Say(s);
			}
		}
	}
		
/*«Speech utilities, Say, SaySt and SayBuf
  * Speech Utilities
  */
 
 
VOID SayBuf(b,l)
FAST TEXT *b;
FAST COUNT l;
	{
	Handle output;
	IMPORT SpeechHandle theSpeech;
	
	TEXT *malloc();
	output = NewHandle(0L);
	Reader(theSpeech, b, (LONG)strlen(b), output);
	MacinTalk(theSpeech, output);
	DisposHandle(output);
	}
	
VOID Say(s)
FAST TEXT *s;
	{
	SayBuf(s, strlen(s));
	}
	
VOID SayHandle(h)
TEXT  **h;
	{
	HLock(h);
	SayBuf((*h)+1, (COUNT)**h);
	HUnlock(h);
	}
	
	
