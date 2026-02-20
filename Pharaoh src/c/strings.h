/*
  * strings.h -- the macros for string lists
  *
  * ST_ 	are string lists for random phrases
  * TM_	are string lists of sprintf templates
  */
  
#define ST_PLAGUES	128	/* the list of plauges */
#define TM_PLAGUES	129 	/* the plauge message template */

#define TM_LOCUSTS	130	/* Locusts devour crop */

#define TM_AOG		131	/* template for acts of god */
#define ST_AOGADJ		132	/* Acts of god adjectives */
#define ST_AOGWHAT	133 	/* Acts of god, events */
#define ST_AOGDOES	134	/* Acts of god, consequences */

#define TM_AOM		140	/* Template for the Acts of Mobs */
#define ST_AOMADJ	141	/* Adjective for the acts of Mobs */
#define ST_AOMWHO	142	/* Who is the mob */
#define ST_AOMMOT	143	/* what motivates the mob */
#define ST_AOMDOES	144	/* what the mob does */

#define TM_WAR		150	/* the WAR template */
#define ST_WARWHO	151	/* the attacking army */
#define ST_WARLOSE	152	/* Synonyms for "you lost" */
#define ST_WARWON	153	/* Synonyms for "You Won" */

#define TM_REVOLT		160	/* the slave revolt template */

#define TM_WORKLOAD	170	/* template for the workload event */

#define TM_HEALTH		180	/* template for health events */

#define TM_LABOR		190	/* template for labor events */

#define TM_WHEAT		200 /* template for wheat events */

#define TM_GOLD		210 /* template for gold events */

#define TM_ECONOMY	220	/* template for economy events */

/*«Error Messages»
  * Error Messages
  */
 
/* buysell.c */
#define ST_BSIN	1000	/* invalid number format */
#define ST_BSCF	1001	/* Choose Function */
#define ST_BSSELL	1002	/* You may not sell more than %f bushells */
#define ST_BSBUY	1003	/* You may not buy more than %f bushells */
#define ST_BSOUT 	1004	/* market can only spare %.0f */
#define ST_BSFULL	1005	/* market can only accept %.0f */

/* compute.c */
#define ST_CMPAY	1020	/* Payroll missed Overseers quit.  %5.1f%% raise */
#define ST_CMCASH	1021	/* Not enough cash, automatic loan */
#define ST_CMRUPT	1022	/* Bankrupt, game over */
#define ST_CMFCLS	1023	/* forclose. */
#define ST_CMDBWN 1024	/* Debt Warning...  Debt_asset ratio becomming unsupportable */

/* feed.c */
#define ST_FDIN	1040	/* invalid number format*/
#define ST_FDNN	1041	/* negative feed rate */

/* file.c */
#define ST_FLSAVE	1060	/* save old game? */
#define ST_FLTYPE	1061	/* selected file has wrong file type */
#define ST_FLPRINT	1062	/* Can't print */
#define ST_FLMANY	1063	/* More than 1 file selected */

/* loan.c */
#define ST_LNIN	1080	/* bad number format */
#define ST_LNSF	1081	/* Select function */
#define ST_LNOVER	1082	/* You have paid more than you have */
#define ST_LNPAID	1083	/* loan repaid */
#define ST_LNEXC	1085	/* credit limit exceeded, reassesment will cost %.0f */
#define ST_LNGRANT 1086	/* Loan of %.0f granted at %0.2f interest */
#define ST_LNSORRY 1087	/* Sorry, no loan */
#define ST_LNNN	1088	/* negative amount */

/* overseer.c */
#define ST_OVIN	1100	/* invalid number format */
#define ST_OVSF	1101	/* select function */
#define ST_OVFRAC	1102	/* fractional overseers */
#define ST_OVFIRE	1103	/* fire more than you have */

/* Plant.c */
#define ST_PLIN	1120	/* invalid number format */
#define ST_PLNN	1121	/* negative wheat */

/* quota.c */
#define ST_QUIN	1140	/* invalid number format */
#define ST_QUNN	1141	/* negative rocks */

/* run.c */
#define ST_RNSAVE	1160	/* save old file before quitting */

/* spread.c */
#define ST_SPIN	1180	/* invalid number format */
#define ST_SPNN	1181	/* negative fertilizer */

/* contract.c */
#define ST_CNPLAY	1200	/* The names of the players.  */
#define ST_CNNENF	1201	/* Not enough good to ship against contract */
#define ST_CNBUY	1202	/* buy contract complete */
#define ST_CNDFLT	1203	/* defaulting on contract */
#define ST_CNNBUY	1204	/* contract issuer cannot buy */
#define ST_CNNSELL	1205	/* contract issuer cannot sell enought */
#define ST_CNNODO	1206	/* pharaoh does not have enought money to buy */
#define ST_CNSELL	1207	/* sell contract is complete */

/*«Vocal messages»
  * Vocal Messages
  */
  
#define VC_WELCOME	10000	/* this is the welcome message */

/*«Neighbor messages»
  * Messages from the neighbors and advice
  */
  
#define ST_CHAT		2500	/* nice little neighborly chat */
#define ST_DUNN		2600	/* the banker has something to say */
#define ST_IDLE		2700	/* wake the guy up now. */
#define ST_ADVERT		2800	/* advertisements */
#define ST_YOUWIN		2900	/* congratulations, you win the game */
#define ST_CONGRATS	2901	/* all the gang say thanks and good bye */

/* 
  * advice:  	All advice categories have a good and bad part.  The good part tells the pharaoh
  *			that he is doing well in that category, while the bad part says he is doing poorly.
  *			it is always possible to transform the good to bad, and vice versa by flipping the
  *			lower order bit of the code.  i.e.  ST_GDOXFD ^ 1 == ST_BDOXFD.
  */
  
  
#define ST_GDOXFD		2000	/* oxen look well fed */
#define ST_BDOXFD		2001	/* oxen look poorly fed */

#define ST_GDSLFD		2010	/* slaves look well fed */
#define ST_BDSLFD		2011	/* slaves don't look well fed */

#define ST_GDHSFD		2020	/* horses look well fed */
#define ST_BDHSFD		2021	/* horses don't look well fed */

#define ST_GDOV		2030	/* plenty of overseers */
#define ST_BDOV		2031	/* not enough overseers */

#define ST_GDST		2040	/* overseers not stressful */
#define ST_BDST		2041	/* overssers are under stress */

#define ST_GDMN		2050	/* good manure per acre ratio */
#define ST_BDMN		2051	/* not enough manure per acre */

#define ST_GDSLHL		2060	/* slaves look healthy */
#define ST_BDSLHL		2061	/* slaves look sick */

#define ST_GDOXHL		2070	/* oxen look healthy */
#define ST_BDOXHL		2071	/* oxen look sick */

#define ST_GDHSHL		2080	/* horses look healthy */
#define ST_BDHSHL		2081	/* horses look sick */

#define ST_GDCRED		2090	/* good credit rating */
#define ST_BDCRED		2091	/* bad credit rating */

