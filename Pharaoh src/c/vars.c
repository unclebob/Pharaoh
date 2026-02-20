/* 
 * This file contains all the definitions for the data variables and tables
 *
 * There are several naming conventions used.  Nouns are represented by dipthongs:
 * 	sl = Slaves
 *	ln = Land 		(usually in acres)
 *	ox = Oxen
 *	hs = Horses
 *	wt = Wheat 	(in bushells)
 * 	mn = Manure 	(in tons)
 *	wk = work 	(in man hours)
 *	ov = overseers
 *	ol = Old value, historical.
 *	py = pyramid
 *
 * Suffixes:
 *	Rt = Rate 		(usually a rate per month)
 *	Eff = Efficiency.  A number between 0 and 1 representing the factor of theoretical maximum.
 *	Health = a number between 0 and 1.  1=healthy, 0=dying.
 *
 * Other Abbreviations
 *	Brth = Birth
 *	Dth = Death
 *	Lash = Slave abuse	(an arbitrary number, 0=none, 1+ = deadly)
 *
 * Other Conventions
 *	Ratios are indicated by the underscore character.  sl_Ov is slaves per overseer.
 *	A preceding t represents an interpolation table.
 *	
 */
 
#include <std.h>
#include "interpolate.h"

/*«Accumulators»
 * These are the accumulators.  They hold values from month to month.
 */
 
 DOUBLE
 	creditLimit = 5e4,	/* Credit Limit */
	creditLower=5e5,	/* smallest allowable credit limit */
 	creditRating = 1,	/* Credit rating, 1=max, 0=min */
 	gold = 0,			/* total fluid capital */
 	horses = 0,		/* The number of horses owned */
	hsHealth = 1,		/* The health of the horses, 1=excellent, 0=dying */
	worldGrowth = .05,	/* anual growth rate of the worlds product demand */
	hsSupply = 1e4,	/* The current number of horses in the market */
	hsDemand = 1e5,	/* The yearly demand for horses */
	hsProduction = 1e5,	/* The yearly production of horses */
	loan = 0.0,		/* current debt */
	lnFallow = 0,		/* Number of acres lying dormant */
	lnGrown = 0,		/* The number of acres containing growing wheat */
	lnRipe = 0,		/* The number of acres containing ripe wheat */
	lnSewn = 0,		/* The number of acres planted this month */
	lnSupply = 1e2,	/* The yearly market supply of land */
	lnDemand = 1e3,	/* The market demand for land */
	lnProduction = 1e3,	/* The number of acres per year made avaliable for sale */
	manure = 0,		/* The number of tons of manure */
	mnSupply = 1e4,	/* The markettable tons of manure */
	mnDemand = 1e5,	/* The yearly market demand for manure */
	mnProduction = 1e5,	/* The yearly production of manure */
	overseers = 0,		/* The number of slave overseers */
	ovPress = 0,		/* The production pressure, or stress, felt by the overseers */
	oxen = 0,			/* The number of oxen to help the slaves */
	oxSupply = 1e4,	/* The market supply of oxen */
	oxDemand = 1e5,	/* The yearly demand for oxen */
	oxProduction = 1e5,	/* The yearly production of oxen */
	oxHealth = 1,		/* The health of the oxen, 1=Excellent, 0=dying */
	pyStones = 0,		/* The number of stones in the pyramid */
	slaves = 0,		/* The number of slaves */
	slHealth = 1,		/* The health of the slaves, 1=Excellent, 0=dying */
	slSupply = 1e3,	/* The market supply of slaves */
	slDemand = 1e4,	/* The yearly market demand for slaves */
	slProduction = 1e4,	/* The yearly production of slaves */
	wheat = 0,		/* The number of bushells of wheat in store */
	wtGrown = 0,		/* The potential bushells of harvested wheat on lnGrown */
	wtRipe = 0,		/* The harvestable wheat from lnRipe (bushells) */
	wtSewn = 0,		/* The potential bushells of harvested wheat on lnSewn */ 
	wtSupply = 1e6,	/* The market supply of wheat */
	wtDemand = 1e7,	/* The yearly market demand for wheat */
	wtProduction = 1e7;	/* The yearly production of wheat */

/*«Time Counters»
  * Time counters
  */
  
DOUBLE month=1, year=1;
	
/*«Variables & Converters»
 * Temporary calculation variables
 */
 
DOUBLE
	debt_asset = 0.0,	/* debt per asset ratio */
	hsAge = 0.0,		/* The nominal loss of horse health due to age etc. */
	hsBrthRt = 0.0,	/* The number of new horses born each month */
	hsDiet = 0.0,		/* The limitted monthly increment of horse health due to diet */
	hsDthRt = 0.0,		/* The number of horses that die each month */
	hsEff_ov = 0.0,	/* The horse efficiency per overseer (sic) */
	hsFed = 0.0,		/* The true number of bushells that the horses were fed this month */
	hs_ov = 0.0,		/* The number of horses per overseer */
	interest = 0.5,		/* Monthly interest percentage rate */
	intAddition = 0.0,	/* Extra interest for credit reasons */
	lnGrowRt = 0.0,	/* The number of planted acres to allow to grow this month */
	lnHvsted = 0.0, 	/* The number of acres to harvest */
	lnRipeRt = 0.0,		/* The number of grown acres to ripen this month */
	lnTotal = 0,		/* The total amount of land */
	maxWk_sl = 0.0,	/* The maximum man-hours that a slave can work this month */
	mnMade = 0.0,		/* The tons of manure produced this month */
	mnSpread = 0.0,	/* The tons of manure that were spread before planting this month */
	mnUsed = 0.0,		/* The rate of loss of manure this month */
	mn_ln =  0,		/* The tons of manure per acre to be spread this month */
	motive = 0.0,		/* The motivation for the slaves.  0=none, 1+= lots. */
	netWth = 0.0,		/* Total worth if all assets liquidated */
	ovEff_sl = 0.0,		/* The effectivness of an overseer spread out over his slaves */
	ovRelax = 0.0,		/* The rate at which overseers shed stress */
	ovStress = 0.0,	/* The increase rate of the quota pressure felt by overseers */
	oxAge = 0.0,		/* The monthly loss of ox health due to age etc. */
	oxBrthRt = 0.0,	/* The monthly number of ox born */
	oxDiet = 0.0,		/* The limitted monthly increase of ox health due to diet */
	oxDthRt = 0.0,		/* The number of oxen that die each month */
	oxFed = 0.0,		/* The number of bushells of wheat actually fed to the oxen */
	oxMult = 0.0,		/* The actual multiplier by which oxen increase a slaves effectivness */
	ox_sl = 0.0,		/* The number of oxen per slave */
	pyAdded = 0.0,		/* The number of stones actually added to the pyramid this month */
	pyHeight = 0.0,		/* The current height of the pyramid */
	reqWk = 0.0,		/* The amount of man-hours required this month */
	reqWk_sl = 0.0,	/* The number of man-hours each slave must produce this month */
	sewRt = 0.0,		/* The number of acres of land to sew with wheat this month */
	slBrthRt = 0.0,		/* The number of slaves born this month */
	slDiet = 0.0,		/* The limmited monthly increment of slave health due to diet */
	slDthRt = 0.0,		/* The number of slaves that died this month */
	slEff = 0.0,		/* Slave efficiency.  1=Max, 0=none */
	slFed = 0.0,		/* actual amount of wheat fed per slave */
	slLabor = 0.0,		/* The number of hours each slave must work each day this month */
	slLashRt = 0.0,		/* The level of abuse of slaves by overseers.  0=none, 1+ = lots */
	slSickRt = 0.0,		/* The amount of slave health lost this month due to various causes */
	sl_ov = 0.0,		/* The number of slaves per overseer */
	sythed = 0.0,		/* The number of bushells of wheat harvested this month */
	totWk = 0.0,		/* The total man hours produced by the slaves this month */
	totWtUsed = 0.0,	/* The total number of bushells of wheat consumed this month */
	wkAddition = 0.0,	/* The temporary addition of total man hours per day */
	wkDeff_sl = 0.0,	/* The number of man hours each slave was deficient this month */
	wkHsTend = 0.0,	/* The man hours per day required to tend the horses this month */
	wkMnSprd = 0.0,	/* The man hours per day required to spread manure this month */
	wkOxTend = 0.0,	/* The man hours per day required to tend the oxen this month */
	wkWtHvst = 0.0,	/* The man hours per day required to harvest wheat this month */
	wkWtSew = 0.0,	/* The man hours per day required to sew wheat this month */
	wkWtTend = 0.0,	/* The man hours per day required to tend the growing wheat */
	wk_sl = 0.0,		/* The number of man hours per day worked by each slave this month */
	wtEaten = 0.0,		/* The number of bushells of wheat eaten by slaves, horses, and oxen */
	wtEff = 0.0,		/* The fraction of the wheat we have vs the wheat we want to use */
	wtFedHs = 0.0,		/* The bushells of wheat fed to the horses this month */
	wtFedOx = 0.0,		/* The bushells of wheat fed to the oxen this month */
	wtFedSl = 0.0,		/* The bushells of wheat fed to the slaves this month */
	wtGrowRt = 0.0,	/* The number of acres planted last month, growing this month */
	wtHrvstd = 0.0,	/* The number of bushells of wheat harvested this month */
	wtLost = 0.0,		/* The bushells of harvestable wheat not harvested this month*/
	wtRipeRt = 0.0,	/* The bushells of wheat becomming harvestable this month */
	wtRotRt = 0.05,	/* The fraction of wheat lost to spoilage each month */
	wtRotted = 0.0,	/* The amount of wheat that spoiled this month */
	wtSewn_ln = 20.0, 	/* The number of bushells of wheat sewn per acre this month */
	wtSewRt = 0.0,	/* The number of bushells of wheat to be sewn this month */
	wtToSew = 0.0,	/* The amount of wheat that the slaves will sew this month */
	wtUsageRt = 0.0;	/* The total amount of wheat used this month */
	
/*«Controls -- Variables under the control of the king»
 * Control Variables
 */
 
 DOUBLE
	hsFeedRt = 0,		/* The bushells of wheat that we want to feed the horses */
	lnToSew = 0,		/* The number of acres we want to sew with wheat this month */
	mnToSprd = 0.0,	/* The tons of manure we wanted to spread this month */
	oxFeedRt = 0,		/* The number of bushells of wheat we want to feed to the oxen */
	pyQuota = 0,		/* The number of stones to add to the pyramid per month */
	pyBase = 300,		/* the number of stones in the base of the pyramid */
 	slFeedRt = 0;		/* The number of bushells to feed each slave this month */


/*«Interpolation Tables»
 * These are the interpolation tables for the pharaoh game
 */
 
Table
	tDebtSupport = /* debt support ratio.  The output of this table reflects the debt to asset
				/* ratio that the bank will be willing to support.
				/**/
				{ 0.0, 1.0,	/* credit rating */
				0.0,.5,.7,.75,.8,.9,1.0,1.3,1.7,2.3,3.0},

	tHsBrthK = 	/* 	The Horse birth factor.  The output of this table represents the fraction
				/*	of total horses that are born per month .
				/**/
				{0.0, 1.0,	/* hsHealth*/
				0.0, .0012, .0027, .0045, .001, .02, .04, .05, .06, .065, .07},
				
	tHsDthK = 		/*	The horse death factor.  The output of this table represents the fraction
				/*	of total horses that dies each month.
				/**/
				{0.0, 1.0, /* hsHealth */
				1.0, .5, .245, .065, .03, .02, .01, .01, .008, .007, .005},
				
	tHsEff=		/*	The efficiency of the horses.  1=Max, 0=none */
				{0.0, 1.0, /* hsHealth */
				0.0, 0.0, .015, .065, .190, .660, .835, .930, .99, 1.0, 1.0},
				
	tHsNourish =	/*	The monthly increase in horse health due to diet */
				{0.0, 75.0, /* hsFed */
				-1., -.1, -.046, .0, .0695, .079, .0865, .092, .0965, .099, 0.1},
				
	tLashSick = 	/*	The monthly loss of health due to the amount of lashes per month */
				{0.0, 100.0, /* slLashRt */
				0.0, .01, .03, .05, .1, .15, .2, .25, .3, .6, 1.0},
				
	tNegMotive =	/*	The amount of slave motivation stimulated by lashes per month */
				{0.0, 100.,	/* slLashRt */
				.0, .1, .2, .3, .35, .38, .42, .45, .47, .48, .5},
				
	tOvEff =		/*	The efficiency of the overseers */
				{0.0, 1.0, /* hsEff_ov */
				.3, .44, .58, .681, .762, .825, .884, .930, .965, .983, .997},
				
	tOxBrthK =		/*	The fraction of total oxen that are born each month */
				{0.0, 1.0, /* oxHealth */
				0.0,.0009,.00285,.00795, .0159, .0280, .0380, .05, .06, .065, .07},
				
	tOxDthK = 		/*	The fraction of total oxen that die each month */
				{0.0, 1.0, /* oxHealth */
				1.0, 0.5, .216, .0959, .0559, .031, .021, .01, .009, .005, .004},
				
	tOxEff = 		/*	The efficiency of the oxen.  0=none, 1=maximum */
				{0.0, 1.0, /* oxHealth */
				0.0, 0.2, .1, .23, .4, .7, .87, .94, .965, .985, 1.0},
				
	tOxMultK =		/*	The factor by which oxen can multiply the effectivness of slaves */
				{0.0, 1.0, /* ox_sl */
				1.0, 1.44, 1.89, 2.27, 2.65, 3.0, 3.27, 3.5, 3.72, 3.88, 4.0},
				
	tOxNourish = 	/* 	The amount of increase in ox health due to diet */
				{0.0, 100.0, /* oxFed */
				-1., -.1, -.0055, .0, .044, .068, .0825, .0915, .0960, .0980, .1},
	
	tPosMotive =	/* 	The amount of slave motivation due to the presence of overseers */
				{0.0, 0.1, /* ovEff_sl */
				.0, .1, .2, .3, .4, .45, .52, .6, .63, .66, .7},
				
	tSeasonYeild = 	/* The factor of wheat yeild based on month */
				{1.0, 12.0,
				.2, .35, .5, .8, 1.0, 1.5, 1.0, .8, .55, .4, .25},
				
	tSlBrthK =	/*	The fraction of total slaves born each month */
				{0.0, 1.0, /* slHealth */
				0.0, .0021, .007, .0161, .0364, .0644, .0980, .121, .134, .139, .14},
				
	tSlDthK =		/*	The fraction of total slaves that die each month */
				{0.0, 1.0, /* slHealth */
				1.0, .485, .235, .135, .0855, .0605, .0405, .0255, .0155, .0105, .002},
				
	tSlNourish =	/* 	The monthly increase in slave health due to diet */
				{0.0, 10.0, /* slFeedRt */
				-1.0, -.5, -.185, .036, .0565, .074, .0865, .098, 0.12, .25, .18},
				
	tStressLash =	/*	The number of slave-lashes generated by overseer stress */
				{0.0, 10.0, /*ovPress (pressure) */
				0.0, 20., 80., 150., 300., 500., 600., 700., 800., 900., 1000.},
				
	tWkAble_sl =	/*	This is the number of man-hours of work that a slave is capable of */
				{0.0, 1.0, /* slHealth */
				0., 1., 5., 10., 14., 15., 17., 18., 19., 19.5, 20.},
				
	tWkSick = 		/*	This is the monthly loss of slave health due to workload */
				{0.0, 24.0, /* slLabor */
				0.0, .0005, .0015, .002, .005, .015, .03, .1, .25, .5, 1.0},
				
	tWtYeild =		/* The potential number of bushells of harvest from 1 bushell of seed */
				{0.0, 10.0, /* mn_ln */
				20.0, 35.0 ,70.0 ,100.0, 150.0, 200.0, 180.0, 140.0, 100.0, 50.0, 0.0};
	
/*«Table variables»
 * These variables hold the value computed from a table
 */
 
 GLOBAL DOUBLE
 	hsBrthK = 0.0,
	hsDthK = 0.0,
	hsEff = 0.0,
	hsNourish = 0.0,
	lashSick = 0.0,
	negMotive = 0.0,
	ovEff = 0.0,
	oxBrthK = 0.0,
	oxDthK = 0.0,
	oxEff = 0.0,
	oxMultK = 0.0,
	oxNourish = 0.0,
	posMotive = 0.0,
	slBrthK = 0.0,
	slDthK = 0.0,
	slNourish = 0.0,
	stressLash = 0.0,
	wkAble_sl = 0.0,
	wkSick = 0.0,
	wtYeild = 0.0;

/* «Historical Commodities»
  *  These variables hold old values for history
  */
  
DOUBLE
	olWt = 0.0,		/* last months wheat */
	olSl = 0.0,		/* last months slaves */
	olHs = 0.0,		/* last months horses */
	olOx = 0.0,		/* last months oxen */
	olMn = 0.0,		/* last months manure */
	oldGold = 0.0;		/* last months gold */
	
/*«Market Prices»
  * Market Prices 
  */
  
DOUBLE
	wtPrice = 	2.0,
	slPrice = 		500.0,
	lnPrice = 		10000.0,
	oxPrice = 	90.0,
	hsPrice = 		100.0,
	mnPrice = 	20.0,
	ovPay = 		300.0,
	inflation = 	.001;

/*«Month translation table»
  * Translation table for month names
  */
  
TEXT *monthName[] =
		{
		"TILT",
		"January",
		"February",
		"March",
		"April",
		"May",
		"June",
		"July",
		"August",
		"September",
		"October",
		"November",
		"December"
		};

/*«Number conversions constants»
  * Number conversion constants
  */
  
TEXT *decades[] =
					{"units ", "teens ", "twenty ", "thirty ", "fourty ", "fifty ", "sixty ",
					"seventy ", "eighty ", "ninety "};
					
TEXT *teens[] =
					{"", "one ", "two ", "three ", "four ", "five ", "six ", "seven ", 
					"eight ", "nine ", "ten ", "eleven ", "twelve ", "thirteen ", "fourteen ",
					"fifteen ", "sixteen ", "seventeen ", "eighteen ", "nineteen "};
