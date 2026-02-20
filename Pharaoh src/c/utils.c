/*
  * Utilities for dialogs et al.
  */
  
#include <std.h>
#include <quickdraw.h>
#include <resource.h>
#include <toolutil.h>
#include <math.h>
#include "random.h"
#include "vars.h"
#include "macintalk.h"

BOOL nagCancel, dunnCancel;

/*
  * IsNumeric(s) -- s points to a string.  If the string is totally numeric, then YES is returned.
  */
  
BOOL IsNumeric(s)
FAST TEXT *s;
	{
	if (*s == NULL)		/* if empty, no good */
		return(NO);
		
	for (; *s; s++)		/* look at each character to see if numeric */
		{
		if (!isdigit(*s) && (*s != '.') && (*s != '+') && (*s != '-') && (tolower(*s) != 'e'))
			return(NO);
		}
			
	return(YES);
	}
	
	
/*«GetIndCString»
  * GetIndCString(s, id, index) -- uses the STR# resource numbered 'id'.  The indexed string is selected
  *					from it, converted to C format, and placed in 's'.  's' is returned.
  *					The index goes from 1 to N where N is the number of strings in the
  *					resource.
  */
  
TEXT *GetIndCString(s, id, index)
FAST TEXT *s;
BITS id;
FAST COUNT index;
	{
	COUNT **handle;
	
	handle = GetResource('STR#', id);
	
	index = min(index, (**handle + 1));
	GetIndString(s, id, index);
	ReleaseResource(handle);
	ptoc(s);
	return(s);
	}
	
/*«GetCString»
  * GetCString(s, id) -- 	uses the STR# resource numbered 'id'.  A string is randomly selected
  *					from it, converted to C format, and placed in 's'.  's' is returned.
  */
  
TEXT *GetCString(s, id)
FAST TEXT *s;
BITS id;
	{
	COUNT **handle, index;
	
	handle = GetResource('STR#', id);
	
	index = URandom(1.0, (DOUBLE)(**handle + 1));
	GetIndString(s, id, index);
	ReleaseResource(handle);
	ptoc(s);
	return(s);
	}


	
/*«Watch»
  * Watch -- Change to Watch cursor
  */

GLOBAL CursHandle  watch;

VOID Watch()
	{
	SetCursor(*watch);
	}
	
	
/*«InitGame»
  * InitGame -- ReInitialize the game
  */
  
VOID InitGame()
	{
	IMPORT VOID SetMen();
 	creditLimit =	50000;	/* credit limit */
	creditLower = 	50000;
	
	slHealth =
	creditRating = 
	oxHealth = 
	hsHealth = 
	month =
	year =	1;

	pyBase = 300;		/* the number of stones in the pyramid base */
	wtPrice = 5;
	slPrice = 500;
	lnPrice = 8000;
	oxPrice = 90;
	hsPrice = 100;
	mnPrice = 20;
	ovPay = 300;
	inflation = .001;  	
	interest = .5;
	
	gold = 
	horses = 
	loan = 
	lnFallow = 
	lnGrown = 
	lnRipe = 
	lnSewn = 
	manure = 
	overseers = 
	ovPress = 
	oxen = 
	pyStones = 
	slaves = 
	wheat =
	wtGrown =
	wtRipe =
	wtSewn =
	hsFeedRt = 
	lnToSew = 
	mnToSprd = 
	oxFeedRt = 
	pyQuota =
	slFeedRt =
	olWt =
	olSl  =
	olHs =
	olOx =
	olMn =
	oldGold =	
	pyHeight = 0;
	
	worldGrowth = .05;
	lnSupply = 1e2;
	lnDemand = lnProduction = 1e3;
	wtSupply = 1e6;
	wtDemand = wtProduction = 1e7;
	mnSupply = 1e4;
	mnDemand = mnProduction = 1e5;
	slSupply = 1e3;
	slDemand = slProduction = 1e4;
	oxSupply = 1e4;
	oxDemand = oxProduction = 1e5;
	hsSupply = 1e4;
	hsDemand = hsProduction = 1e5;
	
	SetMen();
	ClearContracts();
	MakePlayers();
	ContMenuSet();
	InvalRect(&screenBits.bounds);
	nagCancel = dunnCancel = YES;
	}
	
	
/*«Fill»
  * Fill -- Fill a buffer up with a character.  Every byte in the buffer will be set to the value
  *		of the character.
  */
  
TEXT *fill(s,n,c)
FAST TEXT *s;
FAST COUNT n;
FAST TEXT c;
	{
	FAST TEXT *save = s;
	while (n--)
		*s++ = c;
	
	return(save);
	}
	
	
/*«Speech utilities, Say, SaySt and SayBuf
  * Speech Utilities
  */
 
 
VOID SayBuf(b,l)
FAST TEXT *b;
FAST COUNT l;
	{
	Handle output;
	TEXT *readBuf;
	IMPORT SpeechHandle theSpeech;
	IMPORT BOOL speechFlag;
	
	if (speechFlag)
		{
		TEXT *malloc();
		output = NewHandle(0L);
		readBuf = malloc(1000); /* this ought to be big enough */
		NumToEng(b, l, readBuf);
		Reader(theSpeech, readBuf, (LONG)strlen(readBuf), output);
		free(readBuf);
		MacinTalk(theSpeech, output);
		DisposHandle(output);
		}
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
	
VOID SaySt(id)
BITS id;
	{
	TEXT wrk[256];
	
	GetCString(wrk, id);
	Say(wrk);
	}
	
	
/*«NumToEng»
  * NumToEng -- translate numbers to english
  */

#define OUT	1	/* not in a number */
#define DIGIT	2	/* we are in a number */
#define DECIMAL 3	/* we have seen a decimal point */

VOID NumToEng(in, l, out)
FAST TEXT *in;
FAST COUNT l;
TEXT *out;
	{
	FAST COUNT i=0, j=0, w=0;
	TEXT wrk[256];
	BITS state;
	
	state = OUT;
	for (i=0; i<l; i++)
		{
		switch (state)
			{
			case OUT:
				if (isdigit(in[i]))
					{
					w=0;
					wrk[w++] = in[i];
					state = DIGIT;
					}
				else
					out[j++] = in[i];
				break;
					
			case DIGIT:
				if (isdigit(in[i]))
					wrk[w++] = in[i];
				else 	/* we have the number, so translate it */
					{
					DOUBLE atof(), num;
					TEXT aNum[500];
					TEXT *p = &aNum;
					wrk[w] = NULL;
					num = atof(wrk);
					if (num == 0)
						{
						TEXT *tag;
						tag = "zero";
						strcpy(&out[j], tag);
						j += strlen(tag);
						}
					else
						{
						NumToA(num, &p);
						strcpy(&out[j], aNum);
						j += strlen(aNum);
						}
					
					if (in[i] == '.')
						state = DECIMAL;
					else
						state = OUT;
					out[j++] = in[i];
					}
				break;
				
			case DECIMAL:
				if (!isdigit(in[i]))
					state = OUT;
				out[j++] = in[i];
				break;
			}
		}
		out[j] = NULL;
	}
					
/*«NumToA»
  * NumToA -- Convert a string of digits into the words they represent
  */

VOID NumToA(num, s)
DOUBLE num;
FAST TEXT **s;
	{
	TEXT *tag;
	DOUBLE fmod();
	IMPORT TEXT *decades[], *teens[];
	
	if (num >=1e18)
		{
		tag = " ,,, (a really big number) ,,, ";
		strcpy(*s, tag);
		(*s) += strlen(tag);
		return;
		}
		
	if (num >= 1e15)
		{
		NumToA(num/1e15, s);
		tag = "quadrillion ";
		strcpy(*s, tag);
		(*s) += strlen(tag);
		num=fmod(num,1e15);
		}
		
	if (num >= 1e12)
		{
		NumToA(num/1e12, s);
		tag = "trillion ";
		strcpy(*s, tag);
		(*s) += strlen(tag);
		num=fmod(num,1e12);
		}
		
	if (num >= 1000000000.)
		{
		NumToA(num/1000000000., s);
		tag = "billion ";
		strcpy(*s, tag);
		(*s) += strlen(tag);
		num=fmod(num,1e9);
		}
		
	if (num >= 1000000.)
		{
		NumToA(num/1000000., s);
		tag = "million ";
		strcpy(*s, tag);
		(*s) += strlen(tag);
		num=fmod(num,1e6);
		}

	if (num >= 1000.)
		{
		NumToA(num/1000., s);
		tag = "thousand ";
		strcpy(*s, tag);
		(*s) += strlen(tag);
		num=fmod(num,1000.);
		}
	
	if (num >=100.)
		{
		NumToA(num/100., s);
		tag = "hundred ";
		strcpy(*s, tag);
		(*s) += strlen(tag);
		num=fmod(num,100.);
		}

	if (num >= 20)
		{
		tag = decades[(COUNT)(num/10)];
		strcpy(*s, tag);
		(*s) += strlen(tag);
		num=fmod(num,10.);
		}
		
	tag = teens[(COUNT)num];
	strcpy(*s, tag);
	(*s) += strlen(tag);
	}
	
	
/*«fmod»
  * fmod(n,b) -- compute modulus of n to base b.
  */
  
DOUBLE fmod(n,b)
DOUBLE n, b;
	{
	DOUBLE k,j;
	
	k = n/b;
	modf(k,&j);	/* get integral part */
	j *= b;		/* now remultiply */
	return(n-j);	/* and return remainder */
	}
	
/*«FmtFloat»
  * FmtFloat(f,b) --	take the floating point number in f and convert it to an ascii buffer in b.
  *				Always try to use %d notation, but default to %e notation if more than
  *				8 characters are needed.  Always attempt to get as much precision as
  *				the space will allow.
  */
  
TEXT *FmtFloat(f,buf)
DOUBLE f;
FAST TEXT *buf;
	{
	COUNT precision;
	
	if (f >= 1e8)
		ftoa(f, buf, 2, 2);
	else
		{
		COUNT i;
		if (f > 1000) precision = 0;
		else if (f > 100) precision = 1;
		else if (f > 10) precision = 2;
		else if (f > 1) precision = 3;
		else precision = 4;
		ftoa(f, buf, precision, 1);
		
		if (precision > 0) /* if there are decimals */
			{
			for (i=strlen(buf)-1; i; i--) /* trim trailing zeroes */
				{
				if (buf[i] == '0')
					buf[i] = NULL;
				else if (buf[i] == '.')
					{
					buf[i] = NULL;
					break;
					}
				else
					break;
				}
			}
		}
	return(buf);
	}
