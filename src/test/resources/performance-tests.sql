-- GET /api/savings
--Hibernate: select distinct saving0_.id as id1_6_, saving0_.account_id as account_2_6_, saving0_.date as date3_6_, saving0_.value_ as value_4_6_ from saving saving0_ left outer join income incomes1_ on saving0_.id=incomes1_.saving_id left outer join income_category incomecate2_ on incomes1_.category_id=incomecate2_.id left outer join expense expenses3_ on saving0_.id=expenses3_.saving_id left outer join expense_category expensecat4_ on expenses3_.category_id=expensecat4_.id where saving0_.account_id=1 and (incomecate2_.id in (2 , 1 , 4 , 3) or expensecat4_.id in (12 , 1 , 6 , 7 , 10 , 3 , 9 , 4 , 5) or (incomes1_.id is null) and (expenses3_.id is null)) order by saving0_.date desc limit ?
--Hibernate: select distinct count(distinct saving0_.id) as col_0_0_ from saving saving0_ left outer join income incomes1_ on saving0_.id=incomes1_.saving_id left outer join income_category incomecate2_ on incomes1_.category_id=incomecate2_.id left outer join expense expenses3_ on saving0_.id=expenses3_.saving_id left outer join expense_category expensecat4_ on expenses3_.category_id=expensecat4_.id where saving0_.account_id=1 and (incomecate2_.id in (2 , 1 , 4 , 3) or expensecat4_.id in (12 , 1 , 6 , 7 , 10 , 3 , 9 , 4 , 5) or (incomes1_.id is null) and (expenses3_.id is null))
--Hibernate: select income0_.id as id1_4_, income0_.account_id as account_2_4_, income0_.date as date3_4_, income0_.description as descript4_4_, income0_.is_planned as is_plann5_4_, income0_.saving_id as saving_i6_4_, income0_.value_ as value_7_4_, income0_.category_id as category8_4_ from income income0_ where income0_.saving_id in (225 , 240 , 177 , 196 , 124 , 148 , 75 , 89 , 100 , 32 , 44 , 51 , 569 , 582 , 583 , 596 , 598 , 601 , 604 , 501 , 508 , 513 , 515 , 522 , 525 , 526 , 531 , 533 , 536 , 545 , 547 , 448 , 450 , 467 , 468 , 469 , 475 , 478 , 486 , 489 , 493 , 496 , 392 , 395 , 398 , 403 , 412 , 415 , 421 , 427 , 432 , 434 , 435 , 437 , 444 , 338 , 342 , 345 , 354 , 360 , 363 , 370 , 373 , 376 , 379 , 381 , 385 , 387 , 390 , 290 , 292 , 304 , 308 , 317 , 322 , 323 , 325 , 326 , 327 , 331 , 249 , 255 , 257 , 258 , 260 , 263 , 266 , 271 , 274 , 275 , 276 , 280 , 281 , 282 , 286 , 202 , 203 , 205 , 214 , 217)
--Hibernate: select incomecate0_.id as id1_5_0_, incomecate0_.account_id as account_2_5_0_, incomecate0_.name as name3_5_0_ from income_category incomecate0_ where incomecate0_.id in (?, ?, ?, ?)
--Hibernate: select expense0_.id as id1_1_, expense0_.account_id as account_2_1_, expense0_.date as date3_1_, expense0_.description as descript4_1_, expense0_.is_planned as is_plann5_1_, expense0_.saving_id as saving_i6_1_, expense0_.value_ as value_7_1_, expense0_.category_id as category8_1_ from expense expense0_ where (expense0_.saving_id in (225 , 240 , 177 , 196 , 124 , 148 , 75 , 89 , 100 , 32 , 44 , 51 , 569 , 582 , 583 , 596 , 598 , 601 , 604 , 501 , 508 , 513 , 515 , 522 , 525 , 526 , 531 , 533 , 536 , 545 , 547 , 448 , 450 , 467 , 468 , 469 , 475 , 478 , 486 , 489 , 493 , 496 , 392 , 395 , 398 , 403 , 412 , 415 , 421 , 427 , 432 , 434 , 435 , 437 , 444 , 338 , 342 , 345 , 354 , 360 , 363 , 370 , 373 , 376 , 379 , 381 , 385 , 387 , 390 , 290 , 292 , 304 , 308 , 317 , 322 , 323 , 325 , 326 , 327 , 331 , 249 , 255 , 257 , 258 , 260 , 263 , 266 , 271 , 274 , 275 , 276 , 280 , 281 , 282 , 286 , 202 , 203 , 205 , 214 , 217)) and (expense0_.category_id in (12 , 1 , 6 , 7 , 10 , 3 , 9 , 4 , 5))
--Hibernate: select expensecat0_.id as id1_2_0_, expensecat0_.account_id as account_2_2_0_, expensecat0_.name as name3_2_0_ from expense_category expensecat0_ where expensecat0_.id in (?, ?, ?, ?, ?, ?)

-- Depending on a number of co-factors, the Postgres query planner starts to consider a btree index for around 5% of all rows or less
-- https://stackoverflow.com/a/34584053
SET enable_seqscan = on; -- off forces to not use full scan

vacuum full;

-- get savings
explain analyze
select
	distinct saving0_.id as id1_6_,
	saving0_.account_id as account_2_6_,
	saving0_.date as date3_6_,
	saving0_.value_ as value_4_6_
from
	saving saving0_
left outer join income incomes1_ on
	saving0_.id = incomes1_.saving_id
left outer join income_category incomecate2_ on
	incomes1_.category_id = incomecate2_.id
left outer join expense expenses3_ on
	saving0_.id = expenses3_.saving_id
left outer join expense_category expensecat4_ on
	expenses3_.category_id = expensecat4_.id
where
	saving0_.account_id = 1
	and (incomecate2_.id in (2 , 1 , 4 , 3)
		or expensecat4_.id in (12 , 1 , 6 , 7 , 10 , 3 , 9 , 4 , 5)
			or (incomes1_.id is null)
				and (expenses3_.id is null))
order by
	saving0_.date desc
limit 500;

explain analyze
select * from saving saving0_
where
	saving0_.account_id = 1
order by saving0_.date desc
limit 500;

-- get incomes
explain analyze
select
	income0_.id as id1_4_,
	income0_.account_id as account_2_4_,
	income0_.date as date3_4_,
	income0_.description as descript4_4_,
	income0_.is_planned as is_plann5_4_,
	income0_.saving_id as saving_i6_4_,
	income0_.value_ as value_7_4_,
	income0_.category_id as category8_4_
from
	income income0_
where
	income0_.saving_id in (225 , 240 , 177 , 196 , 124 , 148 , 
75 , 89 , 100 , 32 , 44 , 51 , 569 , 582 , 583 , 596 , 598 , 
601 , 604 , 501 , 508 , 513 , 515 , 522 , 525 , 526 , 531 , 
533 , 536 , 545 , 547 , 448 , 450 , 467 , 468 , 469 , 475 , 
478 , 486 , 489 , 493 , 496 , 392 , 395 , 398 , 403 , 412 , 
415 , 421 , 427 , 432 , 434 , 435 , 437 , 444 , 338 , 342 , 
345 , 354 , 360 , 363 , 370 , 373 , 376 , 379 , 381 , 385 , 
387 , 390 , 290 , 292 , 304 , 308 , 317 , 322 , 323 , 325 , 
326 , 327 , 331 , 249 , 255 , 257 , 258 , 260 , 263 , 266 , 
271 , 274 , 275 , 276 , 280 , 281 , 282 , 286 , 202 , 203 , 
205 , 214 , 217);

-- get one income
explain analyze
select
	income0_.id as id1_4_,
	income0_.account_id as account_2_4_,
	income0_.date as date3_4_,
	income0_.description as descript4_4_,
	income0_.is_planned as is_plann5_4_,
	income0_.saving_id as saving_i6_4_,
	income0_.value_ as value_7_4_,
	income0_.category_id as category8_4_
from
	income income0_
where
	income0_.saving_id = 225;
