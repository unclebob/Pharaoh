/* 
 * This file contains all the GLOBAL references for the data variables and tables
 *
 * There are several naming conventions used.  Nouns are represented by dipthongs:
 * 	sl = Slaves
 *	ld = Land 		(usually in acres)
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
 
#include "interpolate.h"

/*«Accumulators»
 * These are the accumulators.  They hold values from month to month.
 */
 
 GLOBAL DOUBLE
 	creditLimit,	/* credit limit */
	creditLower,	/* lowest allowed credit limit */
	creditRating,	/* credit rating */
	gold,			/* Fluid capital */
 	horses,		/* The number of horses owned */
	hsHealth,		/* The health of the horses, 1=excellent, 0=dying */
	worldGrowth,	/* anual growth rate of the worlds product demand */
	hsSupply,		/* The current number of horses in the market */
	hsDemand,	/* The yearly demand for horses */
	hsProduction,	/* The yearly production of horses */
	loan,			/* current debt */
	lnFallow,		/* Number of acres lying dormant */
	lnGrown,		/* The number of acres containing growing wheat */
	lnRipe,		/* The number of acres containing ripe wheat */
	lnSewn,		/* The number of acres planted this month */
	lnSupply,		/* The yearly market supply of land */
	lnDemand ,	/* The market demand for land */
	lnProduction,	/* The number of acres per year made avaliable for sale */
	manure,		/* The number of tons of manure */
	mnSupply,	/* The markettable tons of manure */
	mnDemand ,	/* The yearly market demand for manure */
	mnProduction,	/* The yearly production of manure */
	overseers,	/* The number of slave overseers */
	ovPress,		/* The production pressure, or stress, felt by the overseers */
	oxen,		/* The number of oxen to help the slaves */
	oxSupply,		/* The market supply of oxen */
	oxDemand,	/* The yearly demand for oxen */
	oxProduction,	/* The yearly production of oxen */
	oxHealth,		/* The health of the oxen, 1=Excellent, 0=dying */
	pyStones,		/* The number of stones in the pyramid */
	slaves,		/* The number of slaves */
	slSupply,		/* The market supply of slaves */
	slDemand,		/* The yearly market demand for slaves */
	slProduction,	/* The yearly production of slaves */
	slHealth,		/* The health of the slaves, 1=Excellent, 0=dying */
	wheat,		/* The number of bushells of wheat in store */
	wtGrown,		/* The potential bushells of harvested wheat on lnGrown */
	wtRipe,		/* The harvestable wheat from lnRipe (bushells) */
	wtSewn,		/* The potential bushells of harvested wheat on lnSewn */ 
	wtSupply,		/* The market supply of wheat */
	wtDemand,	/* The yearly market demand for wheat */
	wtProduction;	/* The yearly production of wheat */
	
/*«Time Counters»
  * Time counters
  */
  
DOUBLE month, year;
	
	
/*«Variables & Converters»
 * Temporary calculation variables
 */
 
GLOBAL DOUBLE
	debt_asset,	/* debt to asset ratio */
	hsAge,		/* The nominal loss of horse health due to age etc. */
	hsBrthRt,		/* The number of new horses born each month */
	hsDiet,		/* The limitted monthly increment of horse health due to diet */
	hsDthRt,		/* The number of horses that die each month */
	hsEff_ov,		/* The horse efficiency per overseer (sic) */
	hsFed,		/* The true number of bushells that the horses were fed this month */
	hs_ov,		/* The number of horses per overseer */
	interest,		/* percentage rate for monthly interest */
	intAddition,	/* extra interest for credit reasons */
	lnGrowRt,		/* The number of planted acres to allow to grow this month */
	lnHvsted, 		/* The number of acres to harvest */
	lnRipeRt,		/* The number of grown acres to ripen this month */
	lnTotal,		/* The total amount of land */
	maxWk_sl,	/* The maximum man-hours that a slave can work this month */
	mnMade,		/* The tons of manure produced this month */
	mnSpread,	/* The tons of manure that were spread before planting this month */
	mnUsed,		/* The rate of loss of manure this month */
	mn_ln,		/* The tons of manure per acre to be spread this month */
	motive,		/* The motivation for the slaves.  0=none, 1+= lots. */
	netWth,		/* Total worth if all assets liquidated */
	ovEff_sl,		/* The effectivness of an overseer spread out over his slaves */
	ovRelax,		/* The rate at which overseers shed stress */
	ovStress,		/* The increase rate of the quota pressure felt by overseers */
	oxAge,		/* The monthly loss of ox health due to age etc. */
	oxBrthRt ,	/* The monthly number of ox born */
	oxDiet,		/* The limitted monthly increase of ox health due to diet */
	oxDthRt,		/* The number of oxen that die each month */
	oxFed,		/* The number of bushells of wheat actually fed to the oxen */
	oxMult,		/* The actual multiplier by which oxen increase a slaves effectivness */
	ox_sl,		/* The number of oxen per slave */
	pyAdded,		/* The number of stones actually added to the pyramid this month */
	pyHeight,		/* Current height of the pyramid */
	reqWk,		/* The amount of man-hours required this month */
	reqWk_sl,		/* The number of man-hours each slave must produce this month */
	sewRt,		/* The number of acres of land to sew with wheat this month */
	slBrthRt,		/* The number of slaves born this month */
	slDiet,		/* The limmited monthly increment of slave health due to diet */
	slDthRt,		/* The number of slaves that died this month */
	slEff,		/* Slave efficiency.  1=Max, 0=none */
	slFed,		/* acutal amount of wheat fed per slave */
	slLabor,		/* The number of hours each slave must work each day this month */
	slLashRt,		/* The level of abuse of slaves by overseers.  0=none, 1+ = lots */
	slSickRt,		/* The amount of slave health lost this month due to various causes */
	sl_ov,		/* The number of slaves per overseer */
	sythed,		/* The number of bushells of wheat harvested this month */
	totWk,		/* The total man hours produced by the slaves this month */
	totWtUsed,	/* The total number of bushells of wheat consumed this month */
	wkAddition,	/* The temporary addition of total man hours per day */
	wkDeff_sl,	/* The number of man hours each slave was deficient this month */
	wkHsTend,	/* The man hours per day required to tend the horses this month */
	wkMnSprd,	/* The man hours per day required to spread manure this month */
	wkOxTend,	/* The man hours per day required to tend the oxen this month */
	wkWtHvst,	/* The man hours per day required to harvest wheat this month */
	wkWtSew,	/* The man hours per day required to sew wheat this month */
	wkWtTend,	/* The man hours per day required to tend the growing wheat */
	wk_sl,		/* The number of man hours per day worked by each slave this month */
	wtEaten,		/* The number of bushells of wheat eaten by slaves, horses, and oxen */
	wtEff,		/* The fraction of the wheat we have vs the wheat we want to use */
	wtFedHs,		/* The bushells of wheat fed to the horses this month */
	wtFedOx,		/* The bushells of wheat fed to the oxen this month */
	wtFedSl,		/* The bushells of wheat fed to the slaves this month */
	wtGrowRt,	/* The number of acres planted last month, growing this month */
	wtHrvstd,	/* The number of bushells of wheat harvested this month */
	wtLost,		/* The bushells of harvestable wheat not harvested this month*/
	wtRipeRt,		/* The bushells of wheat becomming harvestable this month */
	wtRotRt,		/* The fraction of wheat lost to spoilage each month */
	wtRotted,		/* The amount of wheat that spoiled this month */
	wtSewn_ln, 	/* The number of bushells of wheat sewn per acre this month */
	wtSewRt,		/* The number of bushells of wheat to be sewn this month */
	wtToSew,		/* The amount of wheat that the slaves will sew this month */
	wtUsageRt;	/* The total amount of wheat used this month */
	
/*«Controls -- Variables under the control of the king»
 * Control Variables
 */
 
GLOBAL DOUBLE
	hsFeedRt,		/* The bushells of wheat that we want to feed the horses */
	lnToSew,		/* The number of acres we want to sew with wheat this month */
	mnToSprd,	/* The tons of manure we wanted to spread this month */
	oxFeedRt,		/* The number of bushells of wheat we want to feed to the oxen */
	pyBase,		/* the number of stones in the pyramid base */
	pyQuota,		/* The number of stones to add to the pyramid per month */
 	slFeedRt;		/* The number of bushells to feed each slave this month */

/*«Interpolation Tables»
 * These are the interpolation tables for the pharaoh game
 */
 
GLOBAL Table
	tDebtSupport,	/* 	debt support ratio.  The output of this table reflects the debt to asset
				/* 	ratio that the bank will be willing to support.
				/**/
	tHsBrthK,		/* 	The Horse birth factor.  The output of this table represents the fraction
				/*	of total horses that are born per month .
				/**/
	tHsDthK, 		/*	The horse death factor.  The output of this table represents the fraction
				/*	of total horses that dies each month.
				/**/				
	tHsEff,		/*	The efficiency of the horses.  1=Max, 0=none */
	tHsNourish,	/*	The monthly increase in horse health due to diet */
	tLashSick,	/*	The monthly loss of health due to the amount of slave abuse */
	tNegMotive,	/*	The amount of slave motivation stimulated by slave abuse */
	tOvEff,		/*	The efficiency of the overseers */
	tOxBrthK,		/*	The fraction of total oxen that are born each month */
	tOxDthK, 		/*	The fraction of total oxen that die each month */
	tOxEff, 		/*	The efficiency of the oxen.  0=none, 1=maximum */
	tOxMultK,		/*	The factor by which oxen can multiply the effectivness of slaves */
	tOxNourish,	/* 	The amount of increase in ox health due to diet */
	tPosMotive,	/* 	The amount of slave motivation due to the presence of overseers */
	tSeasonYeild,	/* The factor of wheat yeild based on month */
	tSlBrthK,		/*	The fraction of total slaves born each month */
	tSlDthK,		/*	The fraction of total slaves that die each month */
	tSlNourish,	/* 	The monthly increase in slave health due to diet */
	tStressLash,	/*	The amount of slave abuse generated by overseer stress */
	tWkAble_sl,	/*	This is the number of man-hours of work that a slave is capable of */
	tWkSick, 		/*	This is the monthly loss of slave health due to workload */
	tWtYeild;		/* The potential number of bushells of harvest from 1 bushell of seed */
	
/*«Table variables»
 * These variables hold the value computed from a table
 */
 
 GLOBAL DOUBLE
 	hsBrthK,
	hsDthK,
	hsEff,
	hsNourish,
	lashSick,
	negMotive,
	ovEff,
	oxBrthK,
	oxDthK,
	oxEff,
	oxMultK,
	oxNourish,
	posMotive,
	slBrthK,
	slDthK,
	slNourish,
	stressLash,
	wkAble_sl,
	wkSick,
	wtYeild;
/* «Historical Commodities»
  *  These variables hold old values for history
  */
  
DOUBLE
	olWt,		/* last months wheat */
	olSl ,		/* last months slaves */
	olHs,		/* last months horses */
	olOx,		/* last months oxen */
	olMn,		/* last months manure */
	oldGold;		/* last months gold	*/
	
	
/*«Market Prices»
  * Market Prices 
  */
  
DOUBLE
	wtPrice,
	slPrice,
	lnPrice,
	oxPrice,
	hsPrice,
	mnPrice,
	ovPay,
	inflation;  
	
/*«Month translation table»
  * Translation table for month names
  */
  
TEXT *monthName[];
