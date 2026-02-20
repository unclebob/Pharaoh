/*
  * symstore.c -- 	This module contains the symbol table used for external access to the critical
  *				pharaoh variables.
  */

#include <std.h>
#include <quickdraw.h>
#include <memory.h>
#include "vars.h"
#include "contract.h"

#define SYM_NUMBER	-1	/* the symbol represents a number */
#define SYM_END		0	/* the end of the symbol table */

DOUBLE atof();
DOUBLE banker, goodGuy, badGuy, dumbGuy;

GLOBAL Contract contOffer[MAXOFFERS], contPend[MAXPEND];
GLOBAL Player players[MAXPLAYERS];

LOCAL struct SymTab
	{
	TEXT *name;
	TEXT *value;	/* pointer to something */
	COUNT size;
	} varSyms[] = 
		{
			{"creditLimit",		(TEXT *)&creditLimit,		SYM_NUMBER},
			{"creditLower",	(TEXT *)&creditLower,		SYM_NUMBER},
			{"creditRating",	(TEXT *)&creditRating,		SYM_NUMBER},
			{"gold",			(TEXT *)&gold,			SYM_NUMBER},
			{"horses",		(TEXT *)&horses,			SYM_NUMBER},
			{"hsHealth",		(TEXT *)&hsHealth,			SYM_NUMBER},
			{"loan",			(TEXT *)&loan,			SYM_NUMBER},
			{"lnFallow",		(TEXT *)&lnFallow,			SYM_NUMBER},
			{"lnGrown",		(TEXT *)&lnGrown,			SYM_NUMBER},
			{"lnRipe",			(TEXT *)&lnRipe,			SYM_NUMBER},
			{"lnSewn",		(TEXT *)&lnSewn,			SYM_NUMBER},
			{"manure",		(TEXT *)&manure,			SYM_NUMBER},
			{"overseers",		(TEXT *)&overseers,		SYM_NUMBER},
			{"ovPress",		(TEXT *)&ovPress,			SYM_NUMBER},
			{"oxen",			(TEXT *)&oxen,			SYM_NUMBER},
			{"oxHealth",		(TEXT *)&oxHealth,			SYM_NUMBER},
			{"pyStones",		(TEXT *)&pyStones,			SYM_NUMBER},
			{"slaves",		(TEXT *)&slaves,			SYM_NUMBER},
			{"slHealth",		(TEXT *)&slHealth,			SYM_NUMBER},
			{"wheat",			(TEXT *)&wheat,			SYM_NUMBER},
			{"wtGrown",		(TEXT *)&wtGrown,			SYM_NUMBER},
			{"wtRipe",		(TEXT *)&wtRipe,			SYM_NUMBER},
			{"wtSewn",		(TEXT *)&wtSewn,			SYM_NUMBER},
			{"month",			(TEXT *)&month,			SYM_NUMBER},
			{"year",			(TEXT *)&year,			SYM_NUMBER},
			{"hsFeedRt",		(TEXT *)&hsFeedRt,			SYM_NUMBER},
			{"lnToSew",		(TEXT *)&lnToSew,			SYM_NUMBER},
			{"mnToSprd",		(TEXT *)&mnToSprd,		SYM_NUMBER},
			{"oxFeedRt",		(TEXT *)&oxFeedRt,			SYM_NUMBER},
			{"pyQuota",		(TEXT *)&pyQuota,			SYM_NUMBER},
			{"pyBase",		(TEXT *)&pyBase,			SYM_NUMBER},
			{"pyHeight",		(TEXT *)&pyHeight,			SYM_NUMBER},
			{"slFeedRt",		(TEXT *)&slFeedRt,			SYM_NUMBER},
			{"olWt",			(TEXT *)&olWt,			SYM_NUMBER},
			{"olSl",			(TEXT *)&olSl,				SYM_NUMBER},
			{"olHs",			(TEXT *)&olHs,			SYM_NUMBER},
			{"olOx",			(TEXT *)&olOx,			SYM_NUMBER},
			{"olMn",			(TEXT *)&olMn,			SYM_NUMBER},
			{"oldGold",		(TEXT *)&oldGold,			SYM_NUMBER},
			{"wtPrice",		(TEXT *)&wtPrice,			SYM_NUMBER},
			{"slPrice",		(TEXT *)&slPrice,			SYM_NUMBER},
			{"lnPrice",		(TEXT *)&lnPrice,			SYM_NUMBER},
			{"oxPrice",		(TEXT *)&oxPrice,			SYM_NUMBER},
			{"hsPrice",		(TEXT *)&hsPrice,			SYM_NUMBER},
			{"mnPrice",		(TEXT *)&mnPrice,			SYM_NUMBER},
			{"ovPay",			(TEXT *)&ovPay,			SYM_NUMBER},
			{"inflation",		(TEXT *)&inflation,			SYM_NUMBER},
			{"banker",		(TEXT *)&banker,			SYM_NUMBER},
			{"goodGuy",		(TEXT *)&goodGuy,			SYM_NUMBER},
			{"badGuy",		(TEXT *)&badGuy,			SYM_NUMBER},
			{"dumbGuy",		(TEXT *)&dumbGuy,			SYM_NUMBER},
			{"worldGrowth",	(TEXT *)&worldGrowth,		SYM_NUMBER},
			{"slSupply",		(TEXT *)&slSupply,			SYM_NUMBER},
			{"slDemand",		(TEXT *)&slDemand,		SYM_NUMBER},
			{"slProduction",	(TEXT *)&slProduction,		SYM_NUMBER},
			{"hsSupply",		(TEXT *)&hsSupply,			SYM_NUMBER},
			{"hsDemand",		(TEXT *)&hsDemand,		SYM_NUMBER},
			{"hsProduction",	(TEXT *)&hsProduction,		SYM_NUMBER},
			{"oxSupply",		(TEXT *)&oxSupply,			SYM_NUMBER},
			{"oxDemand",		(TEXT *)&oxDemand,		SYM_NUMBER},
			{"oxProduction",	(TEXT *)&oxProduction,		SYM_NUMBER},
			{"wtSupply",		(TEXT *)&wtSupply,		SYM_NUMBER},
			{"wtDemand",		(TEXT *)&wtDemand,		SYM_NUMBER},
			{"wtProduction",	(TEXT *)&wtProduction,		SYM_NUMBER},
			{"lnSupply",		(TEXT *)&lnSupply,			SYM_NUMBER},
			{"lnDemand",		(TEXT *)&lnDemand,		SYM_NUMBER},
			{"lnProduction",	(TEXT *)&lnProduction,		SYM_NUMBER},
			{"mnSupply",		(TEXT *)&mnSupply,		SYM_NUMBER},
			{"mnDemand",		(TEXT *)&mnDemand,		SYM_NUMBER},
			{"mnProduction",	(TEXT *)&mnProduction,		SYM_NUMBER},
			
			{"contOffer",		(TEXT *)&contOffer,		sizeof(contOffer)},
			{"contPend",		(TEXT *)&contPend,			sizeof(contPend)},
			{"players",		(TEXT *)&players,			sizeof(players)},
			
			{NIL,NIL, SYM_END }
		};
/*«HxDg»
  * HxDg -- Convert a single ascii hex digit to binary
  */
  
COUNT HxDg(c)
FAST TEXT c;
	{
	c = tolower(c);
	if (c >= '0' && c <= '9')
		return(c-'0');
	else
		return(c-'a'+10);
	}
	
/*«HexIn»
  * HexIn --  convert the next two bytes to from HEX to binary
  */

COUNT HexIn(s)
FAST TEXT *s;
	{
	return(16*HxDg(s[0]) + HxDg(s[1]));
	}
	
/*«SymStore»
  * SymStore -- 	This function takes a single parameter which is a string.  The format 
  *				of the string is "SYMBOL:VALUE".  SYMBOL is a valid symbol name
  *				defined in the symbol table.  VALUE is a string.  VALUE
  *				will be converted and stored in the variable referred to by SYMBOL.
  *
  *	if the size of the symbol is positive, then the string is taken to be the appropriate number of
  *    ascii encoded, hexadecimal bytes.  If the size == SYM_NUMBER then the string is taken to
  *	be an ascii representation of a floating point number.
  *				
  *	i.e.   SymStore("olWt:72.3");  Will change the value of olWt to 72.3.
  *
  * 	If the symbol does not exist, then the function returns NO.  Else it returns YES.
  */
  
BOOL SymStore(s)
FAST TEXT *s;
	{
	FAST struct SymTab *t = &varSyms;
	TEXT symName[50], value[2048];
	FAST TEXT *p;

	for (p=&symName; *s && *s != ':'; )
		*p++ = *s++;
	*p = NULL;
	
	if (*s == NULL)
		return(NO);
	for (p=&value, s++; *s; )
		*p++ = *s++;
	*p = NULL;

	for (t=&varSyms; t->size != SYM_END; t++)
		if (strcmp(t->name, &symName) == 0)
			{
			if (t->size == SYM_NUMBER)
				*((DOUBLE *)(t->value)) = atof(&value);
			else if (t->size > 0)
				{
				FAST COUNT n;
				for (n=0; n<t->size; n++)
					((TEXT *)(t->value))[n] = HexIn(value+(n*3));
				} /* t->size > 0 */
			
			else 
				return(NO);
				
			return(YES);
			}
	return(NO);
	}

/*«SymLoad»
  * SymLoad -- 	This function takes a handle to a buffer as its parameter.  The buffer should
  *  				contain a list of symbol definitions followed by newlines:
  *				
  *					SYMBOL:VALUE
  *					SYMBOL:VALUE
  *					SYMBOL:VALUE
  *					
  *		Symload will step through the buffer, loading all the appropriate variables with the 
  *		specified values.  This is particularily handy for loading up a game that had been saved
  *		previously, or for setting up initial conditions.
  *		
  */

VOID SymLoad(h)
Handle h;
	{
	FAST Size size;
	FAST TEXT *p;
	TEXT wrk[2048];
	FAST COUNT i;
	size = GetHandleSize(h);
	if (size  <= 0)
		return;
		
	HLock(h);
	p = *h;
	
	while (size)
		{
		for (i=0; i<(sizeof(wrk)); i++, size--)
			{
			if (p[i] != '\n' && size)
				wrk[i] = p[i];
			else
				{
				wrk[i] = NULL;
				break;
				}
			}
		if (i<sizeof(wrk))
			SymStore(wrk);
		p += i+1;
		}
	HUnlock(h);
	return;
	}
	
/*«DgHx»
  * DgHx -- convert a number between 0 and 15 into an ascii hex digit
  */
  
TEXT DgHx(n)
FAST COUNT n;
	{
	if (n<0)
		return('0');
	if (n < 10)
		return('0'+n);
	else
		return('a'+n-10);
	}

/*«DigHex»
  * DigHex -- convert a byte into two hex characters
  */
  
VOID DigHex(n,s)
FAST COUNT n;
FAST TEXT *s;
	{
	s[0] = DgHx((n&0xf0)>>4);
	s[1] = DgHx(n&0xf);
	s[2] = ' ';
	s[4] = 0;
	}
	
/*«SymDump»
  * SymDump --	Dump all the symbols into a buffer (whose handle was passed as a parameter).
  *				the symbols will be dumped as lines of the format SYMBOL:VALUE.  Suitable
  *				for loading with SymLoad;
  */
  
SymDump(h)
Handle h;
	{
	FAST struct SymTab *t;
	
	for (t=&varSyms; t->size != SYM_END; t++)
		{
		Size oldSize, newSize;
		COUNT length;
		TEXT wrk[2048], *buf;
		
		if (t->size == SYM_NUMBER)
			sprintf(wrk, "%s:%.8g\n", t->name, *((DOUBLE *)(t->value)));
		else
			{
			COUNT wrkLen, n;
			sprintf(wrk, "%s:", t->name);
			wrkLen = strlen(wrk);
			
			for (n=0; n<t->size; n++, wrkLen+=3)
				DigHex(((TEXT *)(t->value))[n], wrk+wrkLen);
			wrk[wrkLen++] = '\n';
			wrk[wrkLen] = NULL;
			}
		
		oldSize = GetHandleSize(h);
		newSize = oldSize + (length = strlen(wrk));
		SetHandleSize(h, newSize);
		HLock(h);
		buf = *h;
		strncpy(buf+oldSize, wrk, length);
		HUnlock(h);
		}
	}