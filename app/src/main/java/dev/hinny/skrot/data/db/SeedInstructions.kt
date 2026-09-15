package dev.hinny.skrot.data.db

/**
 * How to perform each built-in exercise, in both languages, plus a TL;DR of
 * at most three short cues — the ones a coach would call out before the set.
 * Keyed by the catalog's English name; [SeedData.seedIfEmpty] copies them
 * onto the exercise rows and keeps them in sync, like every other
 * definitional field of a built-in.
 *
 * Kept apart from the catalog itself so the exercise list stays scannable.
 */
object SeedInstructions {

    data class Text(
        val en: String,
        val sv: String,
        val cuesEn: List<String>,
        val cuesSv: List<String>,
    )

    /** Cues are written pipe-separated to keep each entry to four lines. */
    private fun t(en: String, sv: String, cuesEn: String, cuesSv: String) =
        Text(en, sv, cuesEn.split('|'), cuesSv.split('|'))

    val byName: Map<String, Text> = mapOf(
        // Chest
        "Bench Press" to t(
            "Lie with feet flat and shoulder blades pulled back and down. Lower the bar to the lower chest with the elbows around 45 degrees from the body, pause briefly, and press back up over the shoulders.",
            "Ligg med fötterna i golvet och skulderbladen ihopdragna och nedåt. Sänk stången till nedre bröstet med armbågarna cirka 45 grader från kroppen, stanna kort och pressa upp över axlarna.",
            "Shoulder blades pinned|Elbows about 45°|Feet flat, drive the floor",
            "Skulderbladen låsta|Armbågar cirka 45°|Fötterna i golvet, tryck ifrån",
        ),
        "Incline Bench Press" to t(
            "Set the bench to 30–45 degrees. Lower the bar to the upper chest with the elbows tucked slightly, then press up and slightly back so the bar finishes over the shoulders.",
            "Ställ bänken i 30–45 grader. Sänk stången till övre bröstet med armbågarna lätt indragna och pressa sedan upp och lite bakåt så stången slutar över axlarna.",
            "Bar to upper chest|Elbows slightly tucked|Finish over the shoulders",
            "Stången till övre bröstet|Armbågarna lätt indragna|Avsluta över axlarna",
        ),
        "Dumbbell Bench Press" to t(
            "Lie back with the dumbbells over the chest, palms forward. Lower them out and down until the elbows are level with the bench, then press up and slightly together without banging them.",
            "Ligg med hantlarna över bröstet, handflatorna framåt. Sänk dem utåt och nedåt tills armbågarna är i höjd med bänken och pressa sedan upp och lätt inåt utan att slå ihop dem.",
            "Elbows level with the bench|Press up and slightly in|Wrists stacked over elbows",
            "Armbågarna i höjd med bänken|Pressa upp och lätt inåt|Handleder rakt över armbågarna",
        ),
        "Incline Dumbbell Press" to t(
            "On a 30–45 degree bench, press the dumbbells from shoulder level to above the upper chest. Keep the shoulder blades down and back and stop just short of locking out.",
            "På en bänk i 30–45 grader, pressa hantlarna från axelhöjd till ovanför övre bröstet. Håll skulderbladen nere och bakåt och stanna strax innan sträckta armar.",
            "Shoulder blades down and back|Stop short of lockout|Control the way down",
            "Skulderbladen nere och bakåt|Stanna innan låsning|Kontrollera nedvägen",
        ),
        "Chest Press Machine" to t(
            "Set the seat so the handles line up with the mid chest. Press forward until the arms are nearly straight, then bring the handles back until you feel a stretch, without letting the weight stack touch.",
            "Ställ sätet så att handtagen är i höjd med mitten av bröstet. Pressa framåt tills armarna är nästan raka och för sedan tillbaka handtagen tills det sträcker, utan att låta vikterna ta i.",
            "Handles at mid chest|Don't let the stack touch|Chest up, back on the pad",
            "Handtagen mitt på bröstet|Låt inte vikterna ta i|Bröstet upp, ryggen mot dynan",
        ),
        "Push-Up" to t(
            "Hands a little wider than the shoulders, body in one straight line from head to heels. Lower until the chest nearly touches the floor with the elbows about 45 degrees out, then press back up.",
            "Händerna lite bredare än axlarna, kroppen i en rak linje från huvud till hälar. Sänk tills bröstet nästan nuddar golvet med armbågarna cirka 45 grader ut och pressa upp igen.",
            "Straight line head to heels|Chest to the floor|Elbows about 45°",
            "Rak linje huvud till hälar|Bröstet mot golvet|Armbågar cirka 45°",
        ),
        "Dip" to t(
            "Support yourself on the bars with arms straight. Lean slightly forward and lower until the upper arms are about parallel to the floor, then press back up without shrugging the shoulders.",
            "Håll dig uppe på räckena med raka armar. Luta lätt framåt och sänk tills överarmarna är ungefär parallella med golvet, pressa sedan upp utan att rycka upp axlarna.",
            "Slight forward lean|Upper arms to parallel|Shoulders down, not shrugged",
            "Lätt framåtlutning|Överarmarna till parallellt|Axlarna nere, inte uppdragna",
        ),
        "Cable Fly" to t(
            "Stand between the cables with a slight forward lean and a small bend in the elbows. Bring the handles together in a wide arc in front of the chest and return slowly until the chest stretches.",
            "Stå mellan kablarna med en lätt framåtlutning och lätt böjda armbågar. För ihop handtagen i en vid båge framför bröstet och gå långsamt tillbaka tills bröstet sträcks.",
            "Slight elbow bend, keep it|Wide arc, squeeze at the middle|Slow stretch on the return",
            "Lätt böjda armbågar, behåll|Vid båge, kläm ihop i mitten|Långsam sträckning tillbaka",
        ),
        "Dumbbell Fly" to t(
            "Lie on a bench with the dumbbells over the chest and a slight bend in the elbows. Open the arms in a wide arc until you feel a stretch across the chest, then bring them back up along the same path.",
            "Ligg på en bänk med hantlarna över bröstet och lätt böjda armbågar. Öppna armarna i en vid båge tills du känner en sträckning över bröstet och för dem sedan tillbaka längs samma väg.",
            "Keep the elbow angle fixed|Stretch, don't drop|Hug a barrel on the way up",
            "Håll armbågsvinkeln fast|Sträck, släpp inte|Krama en tunna på vägen upp",
        ),
        "Pec Deck" to t(
            "Set the seat so the handles or pads sit at chest height. Press the arms together in front of you with a slight bend in the elbows, squeeze, and open back up under control.",
            "Ställ sätet så att handtagen eller dynorna är i bröstets höjd. Pressa ihop armarna framför dig med lätt böjda armbågar, kläm ihop och öppna kontrollerat.",
            "Handles at chest height|Squeeze at the front|Open slowly, no stack bounce",
            "Handtagen i bröstets höjd|Kläm ihop längst fram|Öppna långsamt, låt inget studsa",
        ),

        // Back
        "Deadlift" to t(
            "Stand with the bar over the mid foot, grip just outside the legs, chest up and back flat. Push the floor away and stand up with the bar close to the legs, finishing tall with the hips through; lower it along the same path.",
            "Stå med stången över mitten av foten, greppa strax utanför benen, bröstet upp och ryggen rak. Tryck ifrån golvet och res dig med stången nära benen, avsluta rak med höften fram; sänk längs samma väg.",
            "Bar over mid foot|Flat back, chest up|Push the floor away",
            "Stången över mitten av foten|Rak rygg, bröstet upp|Tryck golvet ifrån dig",
        ),
        "Romanian Deadlift" to t(
            "Start standing with the bar at the hips. Push the hips back and lower the bar down the thighs with a soft knee and a flat back until the hamstrings stretch, then drive the hips forward to stand.",
            "Börja stående med stången vid höften. Skjut höften bakåt och sänk stången längs låren med lätt böjda knän och rak rygg tills baksidan av låren sträcks, driv sedan höften framåt och res dig.",
            "Hips back, not down|Bar sliding down the thighs|Stop when the hamstrings pull",
            "Höften bakåt, inte nedåt|Stången glider längs låren|Stanna när baksidan drar",
        ),
        "Barbell Row" to t(
            "Hinge forward with a flat back until the torso is around 45 degrees or lower, bar hanging under the shoulders. Pull it to the lower ribs leading with the elbows, then lower under control.",
            "Fäll fram med rak rygg tills överkroppen är i cirka 45 grader eller lägre, stången hängande under axlarna. Dra den mot nedre revbenen med armbågarna först och sänk kontrollerat.",
            "Flat back, hinge held|Elbows lead the pull|Bar to the lower ribs",
            "Rak rygg, håll fällningen|Armbågarna leder draget|Stången mot nedre revbenen",
        ),
        "Dumbbell Row" to t(
            "Brace one hand and knee on a bench, back flat. Row the dumbbell to the hip, keeping the elbow close and the shoulder from twisting open, then lower until the arm is fully long.",
            "Stöd en hand och ett knä på en bänk, rak rygg. Ro hanteln mot höften med armbågen nära kroppen och utan att vrida upp axeln, sänk sedan tills armen är helt lång.",
            "Row to the hip|Elbow close, shoulder square|Full stretch at the bottom",
            "Ro mot höften|Armbågen nära, axeln stilla|Full sträckning längst ner",
        ),
        "Seated Cable Row" to t(
            "Sit tall with a slight knee bend and the chest up. Pull the handle to the stomach while drawing the shoulder blades together, then let the arms extend fully without rounding the back.",
            "Sitt rak med lätt böjda knän och bröstet upp. Dra handtaget mot magen och för ihop skulderbladen, låt sedan armarna sträckas helt utan att runda ryggen.",
            "Sit tall, chest up|Squeeze the shoulder blades|Long arms, back stays flat",
            "Sitt rak, bröstet upp|Kläm ihop skulderbladen|Långa armar, ryggen rak",
        ),
        "Machine Row" to t(
            "Chest on the pad, feet planted. Pull the handles back until the elbows pass the torso, squeeze the upper back, and let the arms return all the way forward under control.",
            "Bröstet mot dynan, fötterna stadigt. Dra handtagen bakåt tills armbågarna passerar överkroppen, kläm ihop övre ryggen och låt armarna gå hela vägen fram kontrollerat.",
            "Chest glued to the pad|Elbows past the torso|Control the return",
            "Bröstet mot dynan|Armbågarna förbi kroppen|Kontrollera tillbaka",
        ),
        "T-Bar Row" to t(
            "Straddle the bar, hinge to about 45 degrees with a flat back and grab the handles. Row to the chest with the elbows tight, pause, and lower until the plates nearly touch the floor.",
            "Stå över stången, fäll till cirka 45 grader med rak rygg och greppa handtagen. Ro mot bröstet med armbågarna nära, stanna och sänk tills vikterna nästan nuddar golvet.",
            "Flat back at 45°|Elbows tight to the body|Pause at the top",
            "Rak rygg i 45°|Armbågarna nära kroppen|Stanna i toppen",
        ),
        "Pull-Up" to t(
            "Hang from the bar with an overhand grip a little wider than the shoulders. Pull until the chin clears the bar, leading with the chest, then lower to a full hang.",
            "Häng i stången med överhandsgrepp lite bredare än axlarna. Dra tills hakan passerar stången med bröstet först och sänk till helt hängande.",
            "Start from a dead hang|Chest to the bar|No kipping",
            "Börja från ett rakt häng|Bröstet mot stången|Inget gungande",
        ),
        "Chin-Up" to t(
            "Hang with an underhand grip at about shoulder width. Pull until the chin clears the bar, keeping the elbows in front of the body, then lower fully.",
            "Häng med underhandsgrepp i ungefär axelbredd. Dra tills hakan passerar stången med armbågarna framför kroppen och sänk helt.",
            "Palms toward you|Elbows drive down and in|Full hang each rep",
            "Handflatorna mot dig|Armbågarna ner och in|Fullt häng varje rep",
        ),
        "Lat Pulldown" to t(
            "Sit with the thighs under the pads and grab the bar a little wider than the shoulders. Pull it to the upper chest with a slight lean back, then let it rise until the arms are straight.",
            "Sitt med låren under dynorna och greppa stången lite bredare än axlarna. Dra den till övre bröstet med en lätt bakåtlutning och låt den sedan gå upp tills armarna är raka.",
            "Bar to the upper chest|Slight lean, not a row|Elbows down to the pockets",
            "Stången till övre bröstet|Lätt lutning, ingen rodd|Armbågarna ner mot fickorna",
        ),
        "Assisted Pull-Up Machine" to t(
            "Kneel or stand on the platform and grab the handles. Pull yourself up until the chin passes the handles, then lower fully. More weight on the stack means more help.",
            "Knäböj eller stå på plattan och greppa handtagen. Dra dig upp tills hakan passerar handtagen och sänk sedan helt. Mer vikt på stacken betyder mer hjälp.",
            "More stack = more help|Chin past the handles|Full stretch at the bottom",
            "Mer vikt = mer hjälp|Hakan förbi handtagen|Full sträckning längst ner",
        ),
        "Back Extension" to t(
            "Set the pad just below the hip bones. Lower the torso with a flat back until you feel the hamstrings stretch, then raise it back to a straight line without going past it.",
            "Ställ dynan strax under höftbenen. Sänk överkroppen med rak rygg tills baksidan av låren sträcks och lyft tillbaka till en rak linje utan att gå förbi.",
            "Pad below the hip bones|Flat back the whole way|Stop at straight, don't arch",
            "Dynan under höftbenen|Rak rygg hela vägen|Stanna vid rakt, svanka inte",
        ),
        "Barbell Shrug" to t(
            "Hold the bar in front of the thighs with straight arms. Lift the shoulders straight up toward the ears, hold a second, and lower them fully. Don't roll them.",
            "Håll stången framför låren med raka armar. Lyft axlarna rakt upp mot öronen, håll en sekund och sänk dem helt. Rulla dem inte.",
            "Straight up, not rolled|Hold at the top|Arms stay straight",
            "Rakt upp, inte rullat|Håll i toppen|Armarna raka",
        ),
        "Face Pull" to t(
            "Set the rope at face height. Pull it toward the face with the elbows high and flaring out, finishing with the hands beside the ears, then return slowly.",
            "Ställ repet i ansiktshöjd. Dra det mot ansiktet med höga armbågar som pekar utåt, avsluta med händerna vid öronen och gå långsamt tillbaka.",
            "Elbows high and wide|Hands to the ears|Light weight, slow return",
            "Armbågarna högt och brett|Händerna till öronen|Lätt vikt, långsamt tillbaka",
        ),

        // Shoulders
        "Overhead Press" to t(
            "Stand with the bar on the front of the shoulders, grip just outside them. Brace, press the bar straight up past the face and finish with it over the mid foot and the head pushed slightly through.",
            "Stå med stången på framsidan av axlarna, greppa strax utanför dem. Spänn bålen, pressa stången rakt upp förbi ansiktet och avsluta med den över mitten av foten och huvudet lätt fram.",
            "Brace before you press|Bar path straight up|Head through at the top",
            "Spänn bålen innan pressen|Stången rakt upp|Huvudet fram i toppen",
        ),
        "Seated Dumbbell Press" to t(
            "Sit on an upright bench with the dumbbells at shoulder height, palms forward. Press them up until the arms are nearly straight, then lower under control to ear level.",
            "Sitt på en upprätt bänk med hantlarna i axelhöjd, handflatorna framåt. Pressa upp dem tills armarna är nästan raka och sänk kontrollerat till öronhöjd.",
            "Back flat on the bench|Lower to ear level|Don't flare the ribs",
            "Ryggen mot bänken|Sänk till öronhöjd|Skjut inte ut revbenen",
        ),
        "Shoulder Press Machine" to t(
            "Set the seat so the handles start at about ear level. Press up until the arms are nearly straight, then lower slowly without letting the weight rest at the bottom.",
            "Ställ sätet så att handtagen börjar i ungefär öronhöjd. Pressa upp tills armarna är nästan raka och sänk långsamt utan att låta vikten vila längst ner.",
            "Handles start at ear level|Back against the pad|Slow on the way down",
            "Handtagen börjar vid öronen|Ryggen mot dynan|Långsamt nedåt",
        ),
        "Arnold Press" to t(
            "Start with the dumbbells in front of the shoulders, palms facing you. Press up while rotating the hands so the palms face forward at the top, then reverse the motion on the way down.",
            "Börja med hantlarna framför axlarna, handflatorna mot dig. Pressa upp medan händerna vrids så att handflatorna pekar framåt i toppen, och vänd rörelsen på vägen ner.",
            "Rotate as you press|Smooth, no jerk|Stop short of lockout",
            "Vrid medan du pressar|Mjukt, inga ryck|Stanna innan låsning",
        ),
        "Lateral Raise" to t(
            "Stand with a dumbbell in each hand at the sides and a slight bend in the elbows. Raise the arms out to the side until they are level with the shoulders, then lower slowly.",
            "Stå med en hantel i varje hand längs sidorna och lätt böjda armbågar. Lyft armarna ut åt sidan tills de är i axelhöjd och sänk långsamt.",
            "Lead with the elbows|Stop at shoulder height|No swing from the hips",
            "Led med armbågarna|Stanna i axelhöjd|Ingen sving från höften",
        ),
        "Cable Lateral Raise" to t(
            "Stand side-on to a low pulley with the handle in the far hand. Raise the arm out to shoulder height with a slight elbow bend, then lower slowly against the cable's pull.",
            "Stå med sidan mot en låg trissa med handtaget i den bortre handen. Lyft armen ut till axelhöjd med lätt böjd armbåge och sänk långsamt mot kabelns drag.",
            "Slight elbow bend|Up to shoulder height|Resist on the way down",
            "Lätt böjd armbåge|Upp till axelhöjd|Bromsa på vägen ner",
        ),
        "Front Raise" to t(
            "Hold the dumbbells in front of the thighs. Raise one or both arms straight in front of you to shoulder height with a slight bend in the elbow, then lower under control.",
            "Håll hantlarna framför låren. Lyft en eller båda armarna rakt fram till axelhöjd med lätt böjd armbåge och sänk kontrollerat.",
            "Up to shoulder height|Slight elbow bend|No leaning back",
            "Upp till axelhöjd|Lätt böjd armbåge|Luta inte bakåt",
        ),
        "Rear Delt Fly" to t(
            "Hinge forward with a flat back, dumbbells hanging below the chest. Raise the arms out to the sides with a slight bend in the elbows, squeeze between the shoulder blades, and lower slowly.",
            "Fäll fram med rak rygg, hantlarna hängande under bröstet. Lyft armarna ut åt sidorna med lätt böjda armbågar, kläm ihop mellan skulderbladen och sänk långsamt.",
            "Flat back in the hinge|Elbows slightly bent|Squeeze the shoulder blades",
            "Rak rygg i fällningen|Armbågarna lätt böjda|Kläm ihop skulderbladen",
        ),
        "Reverse Pec Deck" to t(
            "Face the pad with the handles in front of you at shoulder height. Open the arms out and back with a slight bend in the elbows, pause, and return slowly.",
            "Vänd bröstet mot dynan med handtagen framför dig i axelhöjd. För armarna ut och bakåt med lätt böjda armbågar, stanna och gå långsamt tillbaka.",
            "Chest on the pad|Arms out, not down|Pause at the back",
            "Bröstet mot dynan|Armarna ut, inte ner|Stanna längst bak",
        ),
        "Upright Row" to t(
            "Hold the bar with a grip a bit wider than the shoulders. Pull it up along the body, leading with the elbows, to about chest height, then lower slowly. Keep the wrists straight.",
            "Håll stången med ett grepp lite bredare än axlarna. Dra den upp längs kroppen med armbågarna först till ungefär brösthöjd och sänk långsamt. Håll handlederna raka.",
            "Elbows lead, high and wide|Bar close to the body|Stop at chest height",
            "Armbågarna leder, högt och brett|Stången nära kroppen|Stanna i brösthöjd",
        ),

        // Arms
        "Barbell Curl" to t(
            "Stand with the bar at the thighs, underhand grip at shoulder width. Curl it up with the elbows pinned at the sides until the forearms are vertical, then lower fully.",
            "Stå med stången vid låren, underhandsgrepp i axelbredd. Curla upp den med armbågarna låsta vid sidorna tills underarmarna är lodräta och sänk helt.",
            "Elbows pinned at the sides|No swing from the back|Full extension at the bottom",
            "Armbågarna låsta vid sidorna|Ingen sving med ryggen|Full sträckning längst ner",
        ),
        "Dumbbell Curl" to t(
            "Stand with a dumbbell in each hand, palms forward. Curl one or both up with the elbows still, squeeze at the top, and lower until the arms are straight.",
            "Stå med en hantel i varje hand, handflatorna framåt. Curla upp en eller båda med armbågarna stilla, kläm i toppen och sänk tills armarna är raka.",
            "Elbows still|Squeeze at the top|Straight arms at the bottom",
            "Armbågarna stilla|Kläm i toppen|Raka armar längst ner",
        ),
        "Hammer Curl" to t(
            "Hold the dumbbells with the palms facing each other. Curl them up keeping the thumbs on top and the elbows at the sides, then lower under control.",
            "Håll hantlarna med handflatorna mot varandra. Curla upp dem med tummarna uppåt och armbågarna vid sidorna och sänk kontrollerat.",
            "Thumbs up the whole way|Elbows at the sides|Control the lowering",
            "Tummarna upp hela vägen|Armbågarna vid sidorna|Kontrollera sänkningen",
        ),
        "Preacher Curl" to t(
            "Rest the upper arms on the pad with the armpits at the top edge. Curl the bar up without lifting the elbows off the pad, then lower until the arms are almost straight.",
            "Vila överarmarna på dynan med armhålorna vid övre kanten. Curla upp stången utan att lyfta armbågarna från dynan och sänk tills armarna är nästan raka.",
            "Armpits on the pad's edge|Elbows never leave the pad|Almost straight at the bottom",
            "Armhålorna vid dynans kant|Armbågarna kvar på dynan|Nästan raka längst ner",
        ),
        "Cable Curl" to t(
            "Stand facing a low pulley with the bar or handle at the thighs. Curl up with the elbows fixed at the sides, squeeze, and lower slowly against the constant tension.",
            "Stå vänd mot en låg trissa med stången eller handtaget vid låren. Curla upp med armbågarna fasta vid sidorna, kläm och sänk långsamt mot det jämna motståndet.",
            "Elbows fixed at the sides|Constant tension, no rest|Slow on the way down",
            "Armbågarna fasta vid sidorna|Jämn spänning, ingen vila|Långsamt nedåt",
        ),
        "Biceps Curl Machine" to t(
            "Set the seat so the elbows line up with the machine's pivot and rest the upper arms on the pad. Curl the handles up, squeeze, and lower until the arms are nearly straight.",
            "Ställ sätet så att armbågarna är i linje med maskinens led och vila överarmarna på dynan. Curla upp handtagen, kläm och sänk tills armarna är nästan raka.",
            "Elbows on the pivot line|Upper arms stay on the pad|Nearly straight at the bottom",
            "Armbågarna i ledens linje|Överarmarna kvar på dynan|Nästan raka längst ner",
        ),
        "Triceps Pushdown" to t(
            "Stand at a high pulley with the elbows tucked at the sides. Push the bar down until the arms are straight, squeeze, and let it rise until the forearms are just past horizontal.",
            "Stå vid en hög trissa med armbågarna nära sidorna. Tryck ner stången tills armarna är raka, kläm och låt den gå upp tills underarmarna är strax över vågrätt.",
            "Elbows tucked and still|Straight arms at the bottom|Don't let the elbows drift up",
            "Armbågarna nära och stilla|Raka armar längst ner|Låt inte armbågarna vandra upp",
        ),
        "Skull Crusher" to t(
            "Lie on a bench with the bar over the forehead, arms straight. Bend only at the elbows to lower the bar toward the forehead or just behind it, then extend back up.",
            "Ligg på en bänk med stången över pannan, raka armar. Böj bara i armbågarna för att sänka stången mot pannan eller strax bakom och sträck sedan upp igen.",
            "Only the elbows move|Upper arms stay put|Lower to the forehead or behind",
            "Bara armbågarna rör sig|Överarmarna stilla|Sänk till pannan eller bakom",
        ),
        "Overhead Triceps Extension" to t(
            "Hold one dumbbell with both hands above the head. Lower it behind the head by bending the elbows, keeping the upper arms close to the ears, then extend back up.",
            "Håll en hantel med båda händerna över huvudet. Sänk den bakom huvudet genom att böja armbågarna med överarmarna nära öronen och sträck sedan upp igen.",
            "Upper arms by the ears|Elbows point forward|Full stretch behind the head",
            "Överarmarna vid öronen|Armbågarna pekar framåt|Full sträckning bakom huvudet",
        ),
        "Close-Grip Bench Press" to t(
            "Grip the bar about shoulder width. Lower it to the lower chest with the elbows tucked close to the body, then press back up, keeping the wrists straight over the forearms.",
            "Greppa stången i ungefär axelbredd. Sänk den till nedre bröstet med armbågarna nära kroppen och pressa sedan upp med handlederna raka över underarmarna.",
            "Shoulder-width grip|Elbows tucked|Wrists straight",
            "Grepp i axelbredd|Armbågarna nära kroppen|Raka handleder",
        ),
        "Triceps Dip Machine" to t(
            "Sit with the back against the pad and the handles beside the hips. Press down until the arms are straight, then let the handles rise until the elbows are at about 90 degrees.",
            "Sitt med ryggen mot dynan och handtagen vid höfterna. Pressa ner tills armarna är raka och låt sedan handtagen gå upp tills armbågarna är i cirka 90 grader.",
            "Back on the pad|Elbows to 90° on the way up|Shoulders down, not shrugged",
            "Ryggen mot dynan|Armbågarna till 90° på uppvägen|Axlarna nere, inte uppdragna",
        ),
        "Wrist Curl" to t(
            "Sit with the forearms on the thighs, palms up and the wrists just past the knees. Let the dumbbell roll to the fingers, then curl the wrist up as far as it goes.",
            "Sitt med underarmarna på låren, handflatorna upp och handlederna strax förbi knäna. Låt hanteln rulla ner mot fingrarna och curla sedan upp handleden så långt det går.",
            "Forearms stay on the thighs|Let it roll to the fingers|Only the wrist moves",
            "Underarmarna kvar på låren|Låt den rulla mot fingrarna|Bara handleden rör sig",
        ),

        // Legs
        "Squat" to t(
            "Bar on the upper back, feet about shoulder width with the toes turned out slightly. Brace, sit down and back with the knees tracking over the toes until the thighs are at least parallel, then drive up.",
            "Stången på övre ryggen, fötterna i ungefär axelbredd med tårna lätt utåt. Spänn bålen, sätt dig ner och bakåt med knäna i tårnas riktning tills låren är minst parallella och driv upp.",
            "Brace before the descent|Knees track the toes|At least parallel",
            "Spänn bålen innan nedvägen|Knäna i tårnas riktning|Minst parallellt",
        ),
        "Front Squat" to t(
            "Rest the bar on the front of the shoulders with the elbows high. Squat down keeping the torso upright and the elbows up, as deep as you can with a flat back, then drive up.",
            "Vila stången på framsidan av axlarna med höga armbågar. Böj ner med upprätt överkropp och armbågarna uppe, så djupt du kan med rak rygg, och driv upp.",
            "Elbows high the whole way|Torso upright|Sit between the heels",
            "Höga armbågar hela vägen|Upprätt överkropp|Sitt ner mellan hälarna",
        ),
        "Goblet Squat" to t(
            "Hold a dumbbell vertically against the chest with both hands. Squat down between the heels with the elbows inside the knees, keep the chest up, then stand.",
            "Håll en hantel lodrätt mot bröstet med båda händerna. Böj ner mellan hälarna med armbågarna innanför knäna, håll bröstet uppe och res dig.",
            "Weight tight to the chest|Elbows inside the knees|Chest up, full depth",
            "Vikten tätt mot bröstet|Armbågarna innanför knäna|Bröstet upp, fullt djup",
        ),
        "Leg Press" to t(
            "Feet about shoulder width on the platform. Lower the sled until the knees are at about 90 degrees without the lower back lifting off the pad, then press through the whole foot without locking the knees.",
            "Fötterna i ungefär axelbredd på plattan. Sänk släden tills knäna är i cirka 90 grader utan att ländryggen lyfter från dynan, pressa sedan genom hela foten utan att låsa knäna.",
            "Lower back stays on the pad|Don't lock the knees|Push through the whole foot",
            "Ländryggen kvar på dynan|Lås inte knäna|Tryck genom hela foten",
        ),
        "Hack Squat" to t(
            "Shoulders under the pads, feet a little forward on the platform. Lower until the thighs are at least parallel with the knees following the toes, then press back up without locking out.",
            "Axlarna under dynorna, fötterna lite framåt på plattan. Sänk tills låren är minst parallella med knäna i tårnas riktning och pressa upp utan att låsa.",
            "Feet slightly forward|Knees follow the toes|Stop short of lockout",
            "Fötterna lite framåt|Knäna följer tårna|Stanna innan låsning",
        ),
        "Bulgarian Split Squat" to t(
            "Rear foot on a bench, front foot far enough forward that the shin stays near vertical. Lower until the front thigh is about parallel, keeping the torso tall, then drive up through the front heel.",
            "Bakre foten på en bänk, främre foten så långt fram att smalbenet är nära lodrätt. Sänk tills främre låret är ungefär parallellt med upprätt överkropp och driv upp genom främre hälen.",
            "Front shin near vertical|Torso tall|Drive through the front heel",
            "Främre smalbenet nära lodrätt|Överkroppen upprätt|Driv genom främre hälen",
        ),
        "Lunge" to t(
            "Step forward and lower until both knees are at about 90 degrees, the back knee just off the floor. Push through the front foot to return to standing and alternate legs.",
            "Kliv fram och sänk tills båda knäna är i cirka 90 grader, bakre knät strax över golvet. Tryck ifrån med främre foten tillbaka till stående och byt ben.",
            "Both knees to 90°|Front knee over the foot|Push back through the front foot",
            "Båda knäna till 90°|Främre knät över foten|Tryck tillbaka genom främre foten",
        ),
        "Leg Extension" to t(
            "Set the pad just above the ankles and the pivot in line with the knees. Extend the legs until they are straight, hold a second, and lower slowly.",
            "Ställ dynan strax ovanför vristerna och leden i linje med knäna. Sträck benen tills de är raka, håll en sekund och sänk långsamt.",
            "Pivot in line with the knees|Hold at the top|Slow lowering",
            "Leden i linje med knäna|Håll i toppen|Långsam sänkning",
        ),
        "Lying Leg Curl" to t(
            "Lie face down with the pad just above the heels. Curl the heels toward the glutes without lifting the hips off the bench, then lower slowly.",
            "Ligg på mage med dynan strax ovanför hälarna. Curla hälarna mot rumpan utan att lyfta höften från bänken och sänk långsamt.",
            "Hips stay on the bench|Heels to the glutes|Slow on the way down",
            "Höften kvar på bänken|Hälarna mot rumpan|Långsamt nedåt",
        ),
        "Seated Leg Curl" to t(
            "Sit with the thigh pad locked down and the ankle pad just above the heels. Curl the legs under the seat as far as they go, pause, and return slowly.",
            "Sitt med lårdynan låst och vristdynan strax ovanför hälarna. Curla benen in under sätet så långt det går, stanna och gå långsamt tillbaka.",
            "Thigh pad locked|Full curl under the seat|Pause at the end",
            "Lårdynan låst|Full curl under sätet|Stanna i slutläget",
        ),
        "Hip Thrust" to t(
            "Upper back on a bench, bar across the hips, feet flat with the shins vertical at the top. Drive the hips up until the body is a straight line from shoulders to knees, squeeze, and lower.",
            "Övre ryggen mot en bänk, stången över höften, fötterna platt med lodräta smalben i toppen. Driv upp höften tills kroppen är en rak linje från axlar till knän, kläm och sänk.",
            "Chin tucked, ribs down|Shins vertical at the top|Squeeze the glutes hard",
            "Hakan in, revbenen ner|Lodräta smalben i toppen|Kläm rumpan hårt",
        ),
        "Glute Kickback" to t(
            "Attach the cuff to one ankle and face the pulley, holding on for balance. Kick the leg straight back with a slight knee bend, squeeze the glute, and return under control.",
            "Fäst manschetten runt ena vristen och vänd dig mot trissan med stöd för balansen. Sparka benet rakt bakåt med lätt böjt knä, kläm rumpan och gå kontrollerat tillbaka.",
            "Squeeze the glute at the back|Don't arch the lower back|Control the return",
            "Kläm rumpan längst bak|Svanka inte|Kontrollera tillbaka",
        ),
        "Standing Calf Raise" to t(
            "Shoulders under the pads with the balls of the feet on the edge of the step. Rise as high as you can onto the toes, pause, and lower until the heels are below the step.",
            "Axlarna under dynorna med trampdynorna på stegets kant. Gå upp så högt du kan på tårna, stanna och sänk tills hälarna är under steget.",
            "All the way up|Pause at the top|Heels below the step",
            "Hela vägen upp|Stanna i toppen|Hälarna under steget",
        ),
        "Seated Calf Raise" to t(
            "Sit with the pad on the lower thighs and the balls of the feet on the step. Raise the heels as high as they go, pause, and lower into a full stretch.",
            "Sitt med dynan på nedre låren och trampdynorna på steget. Lyft hälarna så högt det går, stanna och sänk till full sträckning.",
            "Pause at the top|Full stretch at the bottom|No bouncing",
            "Stanna i toppen|Full sträckning längst ner|Inget studs",
        ),
        "Kettlebell Swing" to t(
            "Hinge at the hips with the kettlebell swung back between the legs. Snap the hips forward to float the bell to chest height with straight arms, then let it swing back and hinge again.",
            "Fäll i höften med kettlebellen svingad bakåt mellan benen. Skjut höften fram så klotet flyter upp till brösthöjd med raka armar och låt det sedan svinga tillbaka och fäll igen.",
            "Hips snap, arms just hold|Flat back in the hinge|Bell to chest height, no higher",
            "Höften driver, armarna håller|Rak rygg i fällningen|Klotet till brösthöjd, inte högre",
        ),

        // Abs
        "Crunch" to t(
            "Lie on the back with the knees bent and the hands by the head. Curl the shoulders off the floor toward the hips, exhale at the top, and lower slowly.",
            "Ligg på rygg med böjda knän och händerna vid huvudet. Rulla upp axlarna från golvet mot höften, andas ut i toppen och sänk långsamt.",
            "Curl, don't sit up|Exhale at the top|Don't pull on the neck",
            "Rulla, sätt dig inte upp|Andas ut i toppen|Dra inte i nacken",
        ),
        "Sit-Up" to t(
            "Lie with the knees bent and the feet anchored. Sit all the way up with the arms crossed or hands by the head, then lower under control.",
            "Ligg med böjda knän och fötterna fasthållna. Sätt dig hela vägen upp med korsade armar eller händerna vid huvudet och sänk kontrollerat.",
            "Control the way down|Don't yank the neck|Exhale on the way up",
            "Kontrollera nedvägen|Ryck inte i nacken|Andas ut på vägen upp",
        ),
        "Hanging Leg Raise" to t(
            "Hang from the bar with straight arms. Raise the straight legs to at least horizontal by curling the pelvis up, then lower slowly without swinging.",
            "Häng i stången med raka armar. Lyft de raka benen till minst vågrätt genom att rulla upp bäckenet och sänk långsamt utan att gunga.",
            "Curl the pelvis, not just the legs|No swing|Slow lowering",
            "Rulla bäckenet, inte bara benen|Ingen gungning|Långsam sänkning",
        ),
        "Cable Crunch" to t(
            "Kneel facing a high pulley with the rope held beside the head. Crunch the elbows down toward the knees by rounding the spine, keep the hips still, then return slowly.",
            "Knäböj vänd mot en hög trissa med repet vid huvudet. Curla armbågarna ner mot knäna genom att runda ryggen, håll höften stilla och gå långsamt tillbaka.",
            "Round the spine, hips still|Elbows to the knees|Slow return",
            "Runda ryggen, höften stilla|Armbågarna mot knäna|Långsamt tillbaka",
        ),
        "Ab Machine" to t(
            "Set the pads against the chest or shoulders and the feet under the rollers. Crunch forward by rounding the upper body, pause, and return slowly.",
            "Ställ dynorna mot bröstet eller axlarna och fötterna under rullarna. Curla framåt genom att runda överkroppen, stanna och gå långsamt tillbaka.",
            "Round, don't hinge|Pause at the front|Slow on the way back",
            "Runda, fäll inte|Stanna längst fram|Långsamt tillbaka",
        ),
        "Russian Twist" to t(
            "Sit with the knees bent and the torso leaned back, feet on or off the floor. Rotate the shoulders side to side, moving the hands across the body, keeping the chest up.",
            "Sitt med böjda knän och överkroppen bakåtlutad, fötterna på eller ovanför golvet. Vrid axlarna från sida till sida med händerna över kroppen och håll bröstet uppe.",
            "Rotate the shoulders, not just the arms|Chest up|Controlled tempo",
            "Vrid axlarna, inte bara armarna|Bröstet upp|Kontrollerat tempo",
        ),

        // Library expansion — chest
        "Decline Bench Press" to t(
            "Lie on a decline bench with the feet hooked in. Lower the bar to the lower chest with the elbows tucked slightly, then press straight up over the shoulders.",
            "Ligg på en nedåtlutande bänk med fötterna fasthakade. Sänk stången till nedre bröstet med armbågarna lätt indragna och pressa rakt upp över axlarna.",
            "Bar to the lower chest|Elbows slightly tucked|Shoulder blades pinned",
            "Stången till nedre bröstet|Armbågarna lätt indragna|Skulderbladen låsta",
        ),
        "Smith Machine Bench Press" to t(
            "Set the bench so the bar lowers to the lower chest. Unrack, lower with the elbows about 45 degrees out, pause, and press up; rotate to rack at the end.",
            "Ställ bänken så att stången sänks till nedre bröstet. Lyft av, sänk med armbågarna cirka 45 grader ut, stanna och pressa upp; vrid för att haka på i slutet.",
            "Bench placed for the lower chest|Elbows about 45°|Pause, then press",
            "Bänken placerad för nedre bröstet|Armbågar cirka 45°|Stanna, pressa sedan",
        ),
        "Incline Cable Fly" to t(
            "Lie on an incline bench between low pulleys. With a slight bend in the elbows, bring the handles together above the upper chest and lower until the chest stretches.",
            "Ligg på en lutande bänk mellan låga trissor. Med lätt böjda armbågar, för ihop handtagen över övre bröstet och sänk tills bröstet sträcks.",
            "Fixed elbow bend|Meet above the upper chest|Slow stretch down",
            "Fast armbågsvinkel|Mötas över övre bröstet|Långsam sträckning ner",
        ),
        "Machine Fly" to t(
            "Set the seat so the handles are at chest height. Bring the arms together in front of the chest with a slight bend, squeeze, and open slowly without letting the stack touch.",
            "Ställ sätet så att handtagen är i brösthöjd. För ihop armarna framför bröstet med lätt böjda armbågar, kläm och öppna långsamt utan att låta vikterna ta i.",
            "Handles at chest height|Squeeze at the front|Slow open, no stack bounce",
            "Handtagen i brösthöjd|Kläm längst fram|Öppna långsamt, inget studs",
        ),
        "Svend Press" to t(
            "Press a plate between the palms at chest height. Push it straight out until the arms are almost straight while squeezing the plate hard, then bring it back to the chest.",
            "Pressa en viktskiva mellan handflatorna i brösthöjd. Tryck den rakt fram tills armarna är nästan raka medan du klämmer skivan hårt och för den tillbaka till bröstet.",
            "Squeeze the plate the whole time|Press straight out|Light plate is plenty",
            "Kläm skivan hela tiden|Pressa rakt fram|En lätt skiva räcker",
        ),
        "Diamond Push-Up" to t(
            "Hands together under the chest with the thumbs and index fingers touching. Lower the chest to the hands with the elbows close to the body, then press up.",
            "Händerna ihop under bröstet med tummar och pekfingrar mot varandra. Sänk bröstet mot händerna med armbågarna nära kroppen och pressa upp.",
            "Hands under the chest|Elbows tight to the body|Straight body line",
            "Händerna under bröstet|Armbågarna nära kroppen|Rak kroppslinje",
        ),

        // Library expansion — back
        "Sumo Deadlift" to t(
            "Wide stance with the toes turned out, grip inside the legs. Push the knees out, keep the torso upright and drive the floor away until standing, then lower along the same path.",
            "Bred fotställning med tårna utåt, greppa innanför benen. Skjut ut knäna, håll överkroppen upprätt och driv golvet ifrån dig tills du står, sänk sedan längs samma väg.",
            "Knees out over the toes|Torso upright|Push the floor away",
            "Knäna ut över tårna|Överkroppen upprätt|Tryck golvet ifrån dig",
        ),
        "Hex Bar Deadlift" to t(
            "Stand inside the bar with the feet about hip width. Grab the handles, chest up and back flat, and stand up by pushing through the floor, then lower under control.",
            "Stå inuti stången med fötterna i ungefär höftbredd. Greppa handtagen, bröstet upp och ryggen rak, res dig genom att trycka ifrån golvet och sänk kontrollerat.",
            "Chest up, back flat|Push through the whole foot|Lower, don't drop",
            "Bröstet upp, ryggen rak|Tryck genom hela foten|Sänk, släpp inte",
        ),
        "Stiff-Leg Deadlift" to t(
            "Hold the bar at the hips with the knees nearly straight. Hinge at the hips with a flat back, lowering the bar close to the legs until the hamstrings stretch, then stand back up.",
            "Håll stången vid höften med nästan raka knän. Fäll i höften med rak rygg och sänk stången nära benen tills baksidan av låren sträcks, res dig sedan upp igen.",
            "Knees nearly straight|Bar close to the legs|Flat back, stop at the stretch",
            "Nästan raka knän|Stången nära benen|Rak rygg, stanna vid sträckningen",
        ),
        "Rack Pull" to t(
            "Set the pins just below the knees. Take a deadlift stance, brace and stand up with the bar dragging the thighs, squeezing the glutes at the top; lower it to the pins under control.",
            "Ställ pinnarna strax under knäna. Ta marklyftsställning, spänn bålen och res dig med stången längs låren och kläm rumpan i toppen; sänk kontrollerat till pinnarna.",
            "Pins just below the knee|Bar drags the thighs|Squeeze at the top",
            "Pinnarna strax under knät|Stången längs låren|Kläm i toppen",
        ),
        "Good Morning" to t(
            "Bar on the upper back, soft knees. Hinge at the hips with a flat back until the torso is near parallel or the hamstrings stop you, then drive the hips forward to stand.",
            "Stången på övre ryggen, lätt böjda knän. Fäll i höften med rak rygg tills överkroppen är nära vågrätt eller baksidan av låren stoppar dig, driv sedan höften fram och res dig.",
            "Hips back, flat back|Light weight, slow tempo|Stop when the hamstrings stop you",
            "Höften bak, rak rygg|Lätt vikt, långsamt tempo|Stanna när baksidan stoppar dig",
        ),
        "Pendlay Row" to t(
            "Start with the bar on the floor and the torso parallel to the ground. Row the bar explosively to the lower chest, then lower it back to the floor and reset each rep.",
            "Börja med stången på golvet och överkroppen parallell med marken. Ro stången explosivt till nedre bröstet, sänk den tillbaka till golvet och nollställ varje rep.",
            "Torso parallel, stays there|Bar rests on the floor each rep|Explosive pull",
            "Överkroppen vågrät, håll den|Stången på golvet varje rep|Explosivt drag",
        ),
        "Single-Arm Cable Row" to t(
            "Sit or stand facing a low pulley with one handle. Row it to the hip while keeping the torso still, squeeze the shoulder blade back, then let the arm reach fully forward.",
            "Sitt eller stå vänd mot en låg trissa med ett handtag. Ro det mot höften med överkroppen stilla, kläm skulderbladet bakåt och låt armen sträckas helt fram.",
            "Torso still, no twisting|Row to the hip|Full reach forward",
            "Överkroppen stilla, ingen vridning|Ro mot höften|Full sträckning fram",
        ),
        "Inverted Row" to t(
            "Hang under a bar with the body straight and the heels on the floor. Pull the chest to the bar leading with the elbows, keep the hips up, and lower to straight arms.",
            "Häng under en stång med rak kropp och hälarna i golvet. Dra bröstet till stången med armbågarna först, håll höften uppe och sänk till raka armar.",
            "Body straight, hips up|Chest to the bar|Lower to straight arms",
            "Rak kropp, höften uppe|Bröstet till stången|Sänk till raka armar",
        ),
        "Straight-Arm Pulldown" to t(
            "Stand facing a high pulley with the bar at shoulder height and the arms nearly straight. Pull the bar down in an arc to the thighs using the lats, then return slowly.",
            "Stå vänd mot en hög trissa med stången i axelhöjd och nästan raka armar. Dra stången ner i en båge till låren med latsen och gå långsamt tillbaka.",
            "Arms nearly straight the whole way|Pull with the lats, not the arms|Slow return",
            "Nästan raka armar hela vägen|Dra med latsen, inte armarna|Långsamt tillbaka",
        ),
        "Close-Grip Lat Pulldown" to t(
            "Use a narrow handle, palms facing each other. Pull it to the upper chest with a slight lean back and the elbows driving down, then let the arms extend fully.",
            "Använd ett smalt handtag, handflatorna mot varandra. Dra det till övre bröstet med lätt bakåtlutning och armbågarna nedåt, låt sedan armarna sträckas helt.",
            "Handle to the upper chest|Elbows drive down|Full stretch at the top",
            "Handtaget till övre bröstet|Armbågarna driver nedåt|Full sträckning i toppen",
        ),
        "Reverse-Grip Lat Pulldown" to t(
            "Grab the bar underhand at shoulder width. Pull it to the upper chest keeping the elbows in front of the body, squeeze the lats, and return until the arms are straight.",
            "Greppa stången med underhandsgrepp i axelbredd. Dra den till övre bröstet med armbågarna framför kroppen, kläm latsen och gå tillbaka tills armarna är raka.",
            "Underhand, shoulder width|Elbows in front of the body|Straight arms at the top",
            "Underhand, axelbredd|Armbågarna framför kroppen|Raka armar i toppen",
        ),
        "Neutral-Grip Pull-Up" to t(
            "Hang from parallel handles with the palms facing each other. Pull until the chin passes the handles, keeping the elbows close, then lower to a full hang.",
            "Häng i parallella handtag med handflatorna mot varandra. Dra tills hakan passerar handtagen med armbågarna nära kroppen och sänk till helt hängande.",
            "Palms facing each other|Elbows stay close|Full hang each rep",
            "Handflatorna mot varandra|Armbågarna nära|Fullt häng varje rep",
        ),
        "Dumbbell Shrug" to t(
            "Hold a dumbbell in each hand at the sides. Lift the shoulders straight up toward the ears, hold a second, and lower all the way down.",
            "Håll en hantel i varje hand längs sidorna. Lyft axlarna rakt upp mot öronen, håll en sekund och sänk hela vägen ner.",
            "Straight up and down|Hold at the top|Don't roll the shoulders",
            "Rakt upp och ner|Håll i toppen|Rulla inte axlarna",
        ),
        "Hyperextension" to t(
            "Set the pad just below the hip bones and hook the heels in. Lower the torso with a flat back, then raise it until the body is straight, without arching past it.",
            "Ställ dynan strax under höftbenen och haka fast hälarna. Sänk överkroppen med rak rygg och lyft sedan tills kroppen är rak, utan att svanka förbi.",
            "Pad below the hip bones|Flat back down|Straight line at the top, no arch",
            "Dynan under höftbenen|Rak rygg ner|Rak linje i toppen, ingen svank",
        ),

        // Library expansion — shoulders
        "Push Press" to t(
            "Bar on the front of the shoulders. Dip a few inches by bending the knees with the torso upright, then drive up with the legs and press the bar overhead in one motion.",
            "Stången på framsidan av axlarna. Dippa några centimeter genom att böja knäna med upprätt överkropp, driv sedan upp med benen och pressa stången över huvudet i en rörelse.",
            "Short dip, torso upright|Legs drive, arms finish|Lock out over the mid foot",
            "Kort dipp, upprätt överkropp|Benen driver, armarna avslutar|Lås ut över mitten av foten",
        ),
        "Seated Barbell Press" to t(
            "Sit on an upright bench with the bar at the upper chest. Press it straight up past the face to lockout, then lower under control to the chest or chin.",
            "Sitt på en upprätt bänk med stången vid övre bröstet. Pressa den rakt upp förbi ansiktet till låsning och sänk kontrollerat till bröstet eller hakan.",
            "Back on the bench, ribs down|Bar straight up past the face|Full lockout",
            "Ryggen mot bänken, revbenen ner|Stången rakt upp förbi ansiktet|Full låsning",
        ),
        "Landmine Press" to t(
            "Hold the end of a landmine bar at the shoulder with one hand. Press it up and forward until the arm is straight, keeping the core braced, then lower to the shoulder.",
            "Håll änden av en landminestång vid axeln med en hand. Pressa den upp och framåt tills armen är rak med spänd bål och sänk till axeln.",
            "Press up and forward|Brace the core|No lean to the side",
            "Pressa upp och framåt|Spänn bålen|Luta inte åt sidan",
        ),
        "Cable Front Raise" to t(
            "Stand facing away from a low pulley with the handle in front of the thighs. Raise the arm straight forward to shoulder height with a slight bend, then lower slowly.",
            "Stå med ryggen mot en låg trissa med handtaget framför låren. Lyft armen rakt fram till axelhöjd med lätt böjd armbåge och sänk långsamt.",
            "Up to shoulder height|Slight elbow bend|No leaning back",
            "Upp till axelhöjd|Lätt böjd armbåge|Luta inte bakåt",
        ),
        "Cable Reverse Fly" to t(
            "Stand between two high pulleys holding the opposite handles crossed in front. Open the arms out and back with a slight bend in the elbows, squeeze the rear shoulders, and return slowly.",
            "Stå mellan två höga trissor med de motsatta handtagen korsade framför dig. För armarna ut och bakåt med lätt böjda armbågar, kläm bakre axlarna och gå långsamt tillbaka.",
            "Cross the cables|Arms out and back|Squeeze the rear shoulders",
            "Korsa kablarna|Armarna ut och bakåt|Kläm bakre axlarna",
        ),
        "Cuban Press" to t(
            "Start with a light bar or dumbbells hanging in front. Row up to chest height with high elbows, rotate the forearms up so the hands are above the elbows, then press overhead and reverse.",
            "Börja med en lätt stång eller hantlar hängande framför dig. Ro upp till brösthöjd med höga armbågar, vrid underarmarna upp så att händerna är över armbågarna, pressa sedan över huvudet och vänd.",
            "Light weight, three phases|Rotate with high elbows|Slow and deliberate",
            "Lätt vikt, tre faser|Vrid med höga armbågar|Långsamt och avsiktligt",
        ),
        "Plate Front Raise" to t(
            "Hold a plate at the sides with both hands in front of the thighs. Raise it with nearly straight arms to shoulder height or a little above, then lower slowly.",
            "Håll en viktskiva i sidorna med båda händerna framför låren. Lyft den med nästan raka armar till axelhöjd eller lite över och sänk långsamt.",
            "Arms nearly straight|Up to shoulder height|No swing from the hips",
            "Nästan raka armar|Upp till axelhöjd|Ingen sving från höften",
        ),
        "Band Pull-Apart" to t(
            "Hold a band in front of the chest with straight arms, palms down. Pull it apart until it touches the chest, squeezing the shoulder blades together, then return slowly.",
            "Håll ett band framför bröstet med raka armar, handflatorna ner. Dra isär det tills det nuddar bröstet och kläm ihop skulderbladen, gå sedan långsamt tillbaka.",
            "Arms straight|Band to the chest|Squeeze the shoulder blades",
            "Raka armar|Bandet till bröstet|Kläm ihop skulderbladen",
        ),

        // Library expansion — arms
        "EZ Bar Curl" to t(
            "Grip the EZ bar on the angled parts, palms up. Curl with the elbows pinned at the sides until the forearms are vertical, then lower fully without swinging.",
            "Greppa EZ-stången på de vinklade delarna, handflatorna upp. Curla med armbågarna låsta vid sidorna tills underarmarna är lodräta och sänk helt utan att svinga.",
            "Elbows pinned|No swing|Full extension",
            "Armbågarna låsta|Ingen sving|Full sträckning",
        ),
        "EZ Bar Reverse Curl" to t(
            "Grip the EZ bar palms down. Curl it up with the elbows at the sides and the wrists straight, then lower slowly. Expect to use much less weight than a normal curl.",
            "Greppa EZ-stången med handflatorna ner. Curla upp den med armbågarna vid sidorna och raka handleder och sänk långsamt. Räkna med mycket mindre vikt än en vanlig curl.",
            "Palms down, wrists straight|Elbows at the sides|Light weight",
            "Handflatorna ner, raka handleder|Armbågarna vid sidorna|Lätt vikt",
        ),
        "Incline Dumbbell Curl" to t(
            "Sit back on an incline bench with the arms hanging straight down behind the body. Curl the dumbbells up without moving the upper arms, then lower to a full stretch.",
            "Sitt tillbakalutad på en lutande bänk med armarna hängande rakt ner bakom kroppen. Curla upp hantlarna utan att röra överarmarna och sänk till full sträckning.",
            "Arms hang behind the body|Upper arms don't move|Full stretch at the bottom",
            "Armarna hänger bakom kroppen|Överarmarna stilla|Full sträckning längst ner",
        ),
        "Concentration Curl" to t(
            "Sit with the elbow braced against the inside of the thigh. Curl the dumbbell up toward the shoulder, squeeze, and lower until the arm is straight.",
            "Sitt med armbågen stödd mot insidan av låret. Curla upp hanteln mot axeln, kläm och sänk tills armen är rak.",
            "Elbow braced on the thigh|Squeeze at the top|Straight arm at the bottom",
            "Armbågen stödd mot låret|Kläm i toppen|Rak arm längst ner",
        ),
        "Cable Rope Hammer Curl" to t(
            "Hold the rope at a low pulley with the palms facing each other. Curl up keeping the thumbs on top and the elbows at the sides, then lower slowly.",
            "Håll repet vid en låg trissa med handflatorna mot varandra. Curla upp med tummarna uppåt och armbågarna vid sidorna och sänk långsamt.",
            "Thumbs up|Elbows at the sides|Slow lowering",
            "Tummarna upp|Armbågarna vid sidorna|Långsam sänkning",
        ),
        "Cable Overhead Triceps Extension" to t(
            "Face away from a low pulley holding the rope overhead, elbows by the ears. Extend the arms forward and up until straight, then let the rope return behind the head.",
            "Stå med ryggen mot en låg trissa och håll repet över huvudet, armbågarna vid öronen. Sträck armarna fram och upp tills de är raka och låt repet gå tillbaka bakom huvudet.",
            "Elbows by the ears|Full extension|Stretch behind the head",
            "Armbågarna vid öronen|Full sträckning|Sträck bakom huvudet",
        ),
        "Rope Pushdown" to t(
            "Stand at a high pulley with the rope, elbows tucked at the sides. Push down and pull the rope ends apart at the bottom until the arms are straight, then return slowly.",
            "Stå vid en hög trissa med repet, armbågarna nära sidorna. Tryck ner och dra isär repändarna längst ner tills armarna är raka och gå långsamt tillbaka.",
            "Elbows tucked|Split the rope at the bottom|Slow return",
            "Armbågarna nära kroppen|Dra isär repet längst ner|Långsamt tillbaka",
        ),
        "Bench Dip" to t(
            "Hands on the edge of a bench behind you, legs out in front. Lower by bending the elbows straight back until the upper arms are about parallel, then press up.",
            "Händerna på kanten av en bänk bakom dig, benen framför. Sänk genom att böja armbågarna rakt bakåt tills överarmarna är ungefär parallella och pressa upp.",
            "Elbows point straight back|Upper arms to parallel|Shoulders down",
            "Armbågarna rakt bakåt|Överarmarna till parallellt|Axlarna nere",
        ),
        "Barbell Wrist Curl" to t(
            "Sit with the forearms on the thighs or a bench, palms up, wrists hanging over the edge. Let the bar roll to the fingers, then curl the wrists up as far as they go.",
            "Sitt med underarmarna på låren eller en bänk, handflatorna upp och handlederna över kanten. Låt stången rulla ner mot fingrarna och curla sedan upp handlederna så långt det går.",
            "Forearms stay down|Roll to the fingers|Only the wrists move",
            "Underarmarna kvar|Rulla mot fingrarna|Bara handlederna rör sig",
        ),
        "Reverse Wrist Curl" to t(
            "Sit with the forearms on the thighs, palms down and the wrists over the edge. Raise the back of the hands up as far as they go, then lower slowly.",
            "Sitt med underarmarna på låren, handflatorna ner och handlederna över kanten. Lyft handryggen upp så långt det går och sänk långsamt.",
            "Palms down|Light weight|Only the wrists move",
            "Handflatorna ner|Lätt vikt|Bara handlederna rör sig",
        ),
        "Wrist Roller" to t(
            "Hold the roller in front of you with straight arms. Roll the weight up by turning the wrists hand over hand, then roll it back down under control.",
            "Håll rullaren framför dig med raka armar. Rulla upp vikten genom att vrida handlederna hand över hand och rulla sedan ner den kontrollerat.",
            "Arms straight|Hand over hand|Control it back down",
            "Raka armar|Hand över hand|Kontrollera ner",
        ),

        // Library expansion — legs & glutes
        "Smith Machine Squat" to t(
            "Bar on the upper back, feet slightly in front of the bar. Squat until the thighs are at least parallel with the knees over the toes, then drive up without locking hard.",
            "Stången på övre ryggen, fötterna lite framför stången. Böj tills låren är minst parallella med knäna över tårna och driv upp utan att låsa hårt.",
            "Feet slightly forward|At least parallel|Knees over the toes",
            "Fötterna lite framåt|Minst parallellt|Knäna över tårna",
        ),
        "Pistol Squat" to t(
            "Stand on one leg with the other held out in front. Squat down as low as you can with the heel down and the chest up, then stand back up on the same leg.",
            "Stå på ett ben med det andra hållet framför dig. Böj ner så lågt du kan med hälen i golvet och bröstet uppe och res dig på samma ben.",
            "Heel stays down|Chest up|Hold something if you must",
            "Hälen kvar i golvet|Bröstet upp|Håll i något om du måste",
        ),
        "Walking Lunge" to t(
            "Step forward into a lunge with both knees at about 90 degrees, then push through the front foot and step straight into the next lunge with the other leg.",
            "Kliv fram i ett utfall med båda knäna i cirka 90 grader, tryck sedan ifrån med främre foten och kliv direkt in i nästa utfall med andra benet.",
            "Both knees to 90°|Torso tall|Step through, don't stop",
            "Båda knäna till 90°|Överkroppen upprätt|Kliv igenom, stanna inte",
        ),
        "Reverse Lunge" to t(
            "Step one foot back and lower until both knees are at about 90 degrees with the front shin vertical. Push through the front heel to return and alternate.",
            "Kliv bakåt med ena foten och sänk tills båda knäna är i cirka 90 grader med främre smalbenet lodrätt. Tryck genom främre hälen tillbaka och byt ben.",
            "Step back, not forward|Front shin vertical|Drive through the front heel",
            "Kliv bakåt, inte framåt|Främre smalbenet lodrätt|Driv genom främre hälen",
        ),
        "Barbell Lunge" to t(
            "Bar on the upper back. Step forward and lower until both knees are at about 90 degrees, keeping the torso tall, then push back to standing and alternate.",
            "Stången på övre ryggen. Kliv fram och sänk tills båda knäna är i cirka 90 grader med upprätt överkropp, tryck tillbaka till stående och byt ben.",
            "Torso tall under the bar|Both knees to 90°|Controlled step back",
            "Upprätt under stången|Båda knäna till 90°|Kontrollerat kliv tillbaka",
        ),
        "Step-Up" to t(
            "Place one whole foot on a bench or box. Drive through that foot to stand on top without pushing off the floor with the other leg, then step down under control.",
            "Sätt hela ena foten på en bänk eller låda. Driv genom den foten och stå upp utan att skjuta ifrån golvet med andra benet, kliv sedan ner kontrollerat.",
            "Whole foot on the box|Don't push off the floor|Control the step down",
            "Hela foten på lådan|Skjut inte ifrån golvet|Kontrollera nedsteget",
        ),
        "Single-Leg Romanian Deadlift" to t(
            "Stand on one leg with a dumbbell in the opposite hand. Hinge at the hip, extending the free leg back and lowering the weight with a flat back, then return to standing.",
            "Stå på ett ben med en hantel i motsatt hand. Fäll i höften, sträck fria benet bakåt och sänk vikten med rak rygg, res dig sedan upp igen.",
            "Hips square to the floor|Flat back|Free leg reaches back",
            "Höften rak mot golvet|Rak rygg|Fria benet sträcks bakåt",
        ),
        "Nordic Curl" to t(
            "Kneel with the ankles anchored. Lower the body forward as slowly as possible with the hips extended, catching yourself with the hands, then push back or pull up.",
            "Knäböj med vristerna fasthållna. Sänk kroppen framåt så långsamt som möjligt med sträckt höft, ta emot med händerna och tryck tillbaka eller dra upp.",
            "Hips straight, don't fold|As slow as possible|Hands catch at the bottom",
            "Rak höft, fäll inte|Så långsamt som möjligt|Händerna tar emot längst ner",
        ),
        "Glute Ham Raise" to t(
            "Set the pad so the knees are just behind it and the feet locked in. Lower the torso forward with the hips extended, then curl back up using the hamstrings.",
            "Ställ dynan så att knäna är strax bakom den och fötterna låsta. Sänk överkroppen framåt med sträckt höft och curla upp igen med baksidan av låren.",
            "Hips stay extended|Lower slowly|Curl up with the hamstrings",
            "Höften förblir sträckt|Sänk långsamt|Curla upp med baksidan av låren",
        ),
        "Cable Pull-Through" to t(
            "Face away from a low pulley with the rope between the legs. Hinge at the hips letting the rope pull back, then drive the hips forward to stand tall and squeeze the glutes.",
            "Stå med ryggen mot en låg trissa med repet mellan benen. Fäll i höften och låt repet dra bakåt, driv sedan höften fram, stå rak och kläm rumpan.",
            "Hips back, flat back|Hips drive forward|Squeeze at the top",
            "Höften bak, rak rygg|Höften driver fram|Kläm i toppen",
        ),
        "Hip Abduction Machine" to t(
            "Sit with the pads against the outside of the knees. Push the legs apart as far as they go, pause, and bring them back slowly.",
            "Sitt med dynorna mot utsidan av knäna. Tryck isär benen så långt det går, stanna och för dem långsamt tillbaka.",
            "Pause at the widest point|Slow return|Sit tall",
            "Stanna i bredaste läget|Långsamt tillbaka|Sitt rak",
        ),
        "Hip Adduction Machine" to t(
            "Sit with the pads against the inside of the knees, legs apart. Squeeze the legs together, pause, and open back slowly.",
            "Sitt med dynorna mot insidan av knäna, benen isär. Kläm ihop benen, stanna och öppna långsamt igen.",
            "Squeeze together|Pause at the middle|Slow opening",
            "Kläm ihop|Stanna i mitten|Öppna långsamt",
        ),
        "Calf Press on Leg Press" to t(
            "Balls of the feet on the bottom edge of the leg press platform, legs straight but not locked. Press the toes away as far as they go, pause, and let the heels drop into a stretch.",
            "Trampdynorna på nedre kanten av benpressens platta, benen raka men inte låsta. Pressa bort tårna så långt det går, stanna och låt hälarna sjunka till en sträckning.",
            "Knees straight, not locked|Pause at the top|Full stretch down",
            "Raka knän, inte låsta|Stanna i toppen|Full sträckning ner",
        ),
        "Smith Machine Calf Raise" to t(
            "Bar on the upper back with the balls of the feet on a block. Rise as high as possible onto the toes, pause, and lower until the heels are below the block.",
            "Stången på övre ryggen med trampdynorna på en kloss. Gå upp så högt som möjligt på tårna, stanna och sänk tills hälarna är under klossen.",
            "All the way up|Pause at the top|Heels below the block",
            "Hela vägen upp|Stanna i toppen|Hälarna under klossen",
        ),
        "Single-Leg Calf Raise" to t(
            "Stand on one foot on the edge of a step, holding on for balance. Rise as high as you can, pause, and lower until the heel is below the step.",
            "Stå på en fot på kanten av ett steg och håll i något för balansen. Gå upp så högt du kan, stanna och sänk tills hälen är under steget.",
            "Full height at the top|Pause|Heel below the step",
            "Full höjd i toppen|Stanna|Hälen under steget",
        ),
        "Box Jump" to t(
            "Stand facing a box. Swing the arms and jump, landing softly on the whole foot with the knees bent, then stand tall and step down rather than jumping down.",
            "Stå vänd mot en låda. Sving armarna och hoppa, landa mjukt på hela foten med böjda knän, stå sedan rak och kliv ner i stället för att hoppa ner.",
            "Land softly, whole foot|Stand tall on the box|Step down, don't jump",
            "Landa mjukt på hela foten|Stå rak på lådan|Kliv ner, hoppa inte",
        ),

        // Library expansion — core
        "Plank" to t(
            "Forearms on the floor under the shoulders, body in a straight line from head to heels. Squeeze the glutes and brace the abs, breathing normally, for the whole hold.",
            "Underarmarna i golvet under axlarna, kroppen i en rak linje från huvud till hälar. Kläm rumpan och spänn magen och andas normalt under hela hållet.",
            "Straight line, no sag|Squeeze the glutes|Breathe",
            "Rak linje, ingen svank|Kläm rumpan|Andas",
        ),
        "Side Plank" to t(
            "Lie on the side with the elbow under the shoulder. Lift the hips until the body is a straight line and hold, then switch sides.",
            "Ligg på sidan med armbågen under axeln. Lyft höften tills kroppen är en rak linje, håll och byt sida.",
            "Elbow under the shoulder|Hips up, straight line|Don't let the hips sag",
            "Armbågen under axeln|Höften upp, rak linje|Låt inte höften sjunka",
        ),
        "Mountain Climbers" to t(
            "Start in a push-up position. Drive one knee toward the chest and switch legs quickly, keeping the hips low and the shoulders over the hands.",
            "Börja i armhävningsposition. Driv ena knät mot bröstet och byt ben snabbt, håll höften låg och axlarna över händerna.",
            "Hips low|Shoulders over the hands|Quick, even switches",
            "Höften låg|Axlarna över händerna|Snabba, jämna byten",
        ),
        "Dead Bug" to t(
            "Lie on the back with the arms up and the knees over the hips. Press the lower back into the floor, then extend one arm and the opposite leg away, return, and alternate.",
            "Ligg på rygg med armarna upp och knäna över höften. Pressa ländryggen mot golvet, sträck sedan ena armen och motsatta benet bort, gå tillbaka och byt.",
            "Lower back glued to the floor|Opposite arm and leg|Slow and controlled",
            "Ländryggen mot golvet|Motsatt arm och ben|Långsamt och kontrollerat",
        ),
        "Ab Wheel Rollout" to t(
            "Kneel with the wheel under the shoulders. Roll forward as far as you can without the lower back arching, then pull back with the abs to the start.",
            "Knäböj med hjulet under axlarna. Rulla fram så långt du kan utan att ländryggen svankar och dra sedan tillbaka med magen till startläget.",
            "Round the back slightly, no arch|Only as far as you can hold|Pull back with the abs",
            "Runda ryggen lätt, ingen svank|Bara så långt du kan hålla|Dra tillbaka med magen",
        ),
        "Decline Sit-Up" to t(
            "Hook the feet under the pads on a decline bench. Sit up with the hands by the head or across the chest, then lower slowly until the back nearly touches.",
            "Haka fast fötterna under dynorna på en nedåtlutande bänk. Sätt dig upp med händerna vid huvudet eller över bröstet och sänk långsamt tills ryggen nästan nuddar.",
            "Control the lowering|Don't pull on the neck|Exhale on the way up",
            "Kontrollera sänkningen|Dra inte i nacken|Andas ut på vägen upp",
        ),
        "Oblique Crunch" to t(
            "Lie on the back with the knees bent. Crunch up and across, bringing one shoulder toward the opposite knee, then lower and alternate sides.",
            "Ligg på rygg med böjda knän. Curla upp och snett, för ena axeln mot motsatt knä, sänk och byt sida.",
            "Shoulder to the opposite knee|Twist from the ribs|Don't pull on the neck",
            "Axeln mot motsatt knä|Vrid från revbenen|Dra inte i nacken",
        ),
        "Dumbbell Side Bend" to t(
            "Stand with a dumbbell in one hand at the side. Bend sideways toward the weight, then pull back up past upright using the opposite side. Switch hands after the set.",
            "Stå med en hantel i ena handen längs sidan. Böj åt sidan mot vikten och dra sedan upp förbi upprätt med motsatta sidan. Byt hand efter setet.",
            "Straight sideways, no twist|One dumbbell only|Pull up past upright",
            "Rakt åt sidan, ingen vridning|Bara en hantel|Dra upp förbi upprätt",
        ),
        "Plate Twist" to t(
            "Sit leaned back with the knees bent and a plate held in front of the chest. Rotate the torso side to side, moving the plate across the body with the chest up.",
            "Sitt bakåtlutad med böjda knän och en viktskiva framför bröstet. Vrid överkroppen från sida till sida med skivan över kroppen och bröstet uppe.",
            "Rotate the torso, not just the arms|Chest up|Controlled tempo",
            "Vrid överkroppen, inte bara armarna|Bröstet upp|Kontrollerat tempo",
        ),
        "Cable Woodchopper" to t(
            "Stand side-on to a high pulley with both hands on the handle. Pull it down and across the body to the opposite hip, rotating through the torso with the arms nearly straight, then return slowly.",
            "Stå med sidan mot en hög trissa med båda händerna på handtaget. Dra det ner och tvärs över kroppen till motsatt höft, vrid genom överkroppen med nästan raka armar och gå långsamt tillbaka.",
            "Rotate from the torso|Arms nearly straight|Slow on the way back",
            "Vrid från överkroppen|Nästan raka armar|Långsamt tillbaka",
        ),
        "Pallof Press" to t(
            "Stand side-on to a cable at chest height, handle held at the chest. Press it straight out in front and hold without letting the cable twist you, then bring it back.",
            "Stå med sidan mot en kabel i brösthöjd med handtaget vid bröstet. Pressa det rakt fram och håll utan att låta kabeln vrida dig, för det sedan tillbaka.",
            "Press straight out|Resist the twist|Hold, then return",
            "Pressa rakt fram|Stå emot vridningen|Håll, gå sedan tillbaka",
        ),
        "Hanging Knee Raise" to t(
            "Hang from the bar with straight arms. Raise the knees toward the chest by curling the pelvis up, then lower slowly without swinging.",
            "Häng i stången med raka armar. Lyft knäna mot bröstet genom att rulla upp bäckenet och sänk långsamt utan att gunga.",
            "Curl the pelvis up|No swing|Slow lowering",
            "Rulla upp bäckenet|Ingen gungning|Långsam sänkning",
        ),
        "Toes to Bar" to t(
            "Hang from the bar with straight arms. Raise the straight legs all the way up to touch the bar, leading with the hips, then lower under control.",
            "Häng i stången med raka armar. Lyft de raka benen hela vägen upp till stången med höften först och sänk kontrollerat.",
            "Lead with the hips|Legs straight|Control the way down",
            "Led med höften|Raka ben|Kontrollera nedvägen",
        ),

        // Library expansion — full body & conditioning
        "Clean and Jerk" to t(
            "Pull the bar from the floor and catch it on the shoulders in a squat, stand, then dip and drive it overhead, splitting or dipping the legs to catch it with straight arms.",
            "Dra stången från golvet och ta emot den på axlarna i en knäböj, res dig, dippa sedan och driv den över huvudet med benen i split eller dipp för att ta emot med raka armar.",
            "Full extension before the catch|Elbows fast under the bar|Lock out, then stand",
            "Full sträckning innan mottaget|Armbågarna snabbt under stången|Lås ut, res dig sedan",
        ),
        "Power Clean" to t(
            "Pull the bar from the floor, extend the hips explosively and shrug, then drop under it to catch it on the shoulders with the knees only slightly bent.",
            "Dra stången från golvet, sträck höften explosivt och rycka upp axlarna, sjunk sedan under den och ta emot på axlarna med bara lätt böjda knän.",
            "Hips extend before the arms bend|Fast elbows|Catch high, don't squat",
            "Höften sträcks innan armarna böjs|Snabba armbågar|Ta emot högt, böj inte",
        ),
        "Snatch" to t(
            "Wide grip on the bar. Pull from the floor, extend the hips explosively and pull yourself under, catching the bar overhead with straight arms in a squat, then stand.",
            "Brett grepp om stången. Dra från golvet, sträck höften explosivt och dra dig under, ta emot stången över huvudet med raka armar i en knäböj och res dig.",
            "Bar close the whole way|Full extension, then under|Lock the arms in the catch",
            "Stången nära hela vägen|Full sträckning, sedan under|Lås armarna i mottaget",
        ),
        "Thruster" to t(
            "Bar on the front of the shoulders. Squat to full depth, then drive up and use the momentum to press the bar overhead in one continuous motion, and lower back to the shoulders.",
            "Stången på framsidan av axlarna. Böj till fullt djup, driv sedan upp och använd farten för att pressa stången över huvudet i en sammanhängande rörelse, sänk tillbaka till axlarna.",
            "Full squat first|One continuous motion|Elbows up in the squat",
            "Full knäböj först|En sammanhängande rörelse|Armbågarna uppe i knäböjen",
        ),
        "Burpee" to t(
            "From standing, drop the hands to the floor, kick the feet back into a push-up position, do a push-up, jump the feet back in and jump up with the arms overhead.",
            "Från stående, sätt händerna i golvet, sparka bak fötterna till armhävningsposition, gör en armhävning, hoppa in fötterna och hoppa upp med armarna över huvudet.",
            "Chest to the floor|Feet all the way in|Full jump at the top",
            "Bröstet mot golvet|Fötterna hela vägen in|Fullt hopp i toppen",
        ),
        "Farmer's Carry" to t(
            "Pick up a heavy dumbbell in each hand and walk with the shoulders back, chest up and short steps. Set them down with a flat back at the end.",
            "Lyft en tung hantel i varje hand och gå med axlarna bakåt, bröstet uppe och korta steg. Sätt ner dem med rak rygg i slutet.",
            "Shoulders back, chest up|Short quick steps|Don't lean to a side",
            "Axlarna bak, bröstet upp|Korta snabba steg|Luta inte åt sidan",
        ),
        "Turkish Get-Up" to t(
            "Lie down with a kettlebell pressed up in one hand. Roll to the elbow, then the hand, lift the hips, sweep the leg through to kneel and stand, keeping the arm locked overhead; reverse to lie back down.",
            "Ligg med en kettlebell pressad upp i ena handen. Rulla upp på armbågen, sedan handen, lyft höften, svep benet igenom till knästående och res dig med armen låst över huvudet; vänd för att lägga dig igen.",
            "Eyes on the bell|Arm locked the whole way|Slow, one position at a time",
            "Blicken på klotet|Armen låst hela vägen|Långsamt, en position i taget",
        ),
        "Kettlebell Clean and Press" to t(
            "Swing the kettlebell between the legs and pull it to the rack position at the shoulder, keeping it close to the body, then press it overhead and lower back to the rack.",
            "Sving kettlebellen mellan benen och dra upp den till rackposition vid axeln nära kroppen, pressa den sedan över huvudet och sänk tillbaka till racket.",
            "Bell close to the body on the clean|Soft catch at the shoulder|Press, don't push-press",
            "Klotet nära kroppen i cleanen|Mjukt mottag vid axeln|Pressa, ingen push press",
        ),
        "Kettlebell Goblet Squat" to t(
            "Hold the kettlebell by the horns at the chest. Squat down between the heels with the elbows inside the knees and the chest up, then stand.",
            "Håll kettlebellen i hornen vid bröstet. Böj ner mellan hälarna med armbågarna innanför knäna och bröstet uppe och res dig.",
            "Bell tight to the chest|Elbows inside the knees|Chest up, full depth",
            "Klotet tätt mot bröstet|Armbågarna innanför knäna|Bröstet upp, fullt djup",
        ),
        "Kettlebell Snatch" to t(
            "Swing the kettlebell back between the legs, drive the hips and pull it up close to the body, punching the hand through at the top so the bell settles on the forearm overhead.",
            "Sving kettlebellen bakåt mellan benen, driv höften och dra upp den nära kroppen, stöt handen igenom i toppen så att klotet lägger sig på underarmen över huvudet.",
            "Hips drive it|Keep it close|Punch through at the top",
            "Höften driver|Håll det nära|Stöt igenom i toppen",
        ),
        "Resistance Band Row" to t(
            "Anchor the band in front of you at chest height or under the feet. Row the handles to the ribs with the elbows close, squeeze the shoulder blades, and return slowly.",
            "Fäst bandet framför dig i brösthöjd eller under fötterna. Ro handtagen mot revbenen med armbågarna nära, kläm ihop skulderbladen och gå långsamt tillbaka.",
            "Elbows close|Squeeze the shoulder blades|Slow return",
            "Armbågarna nära|Kläm ihop skulderbladen|Långsamt tillbaka",
        ),
        "Band Lateral Walk" to t(
            "Band around the ankles or just above the knees, feet hip width and knees slightly bent. Step sideways keeping tension on the band, then back the other way.",
            "Bandet runt vristerna eller strax ovanför knäna, fötterna i höftbredd och lätt böjda knän. Kliv i sidled med spänning i bandet och sedan tillbaka åt andra hållet.",
            "Keep tension on the band|Knees slightly bent|Small, controlled steps",
            "Håll spänning i bandet|Lätt böjda knän|Små, kontrollerade steg",
        ),
        "Neck Flexion" to t(
            "Lie on the back on a bench with the head over the edge and a light plate on the forehead, held with a towel. Curl the chin toward the chest, then lower slowly.",
            "Ligg på rygg på en bänk med huvudet över kanten och en lätt viktskiva på pannan, hållen med en handduk. Curla hakan mot bröstet och sänk långsamt.",
            "Very light weight|Chin to the chest|Slow both ways",
            "Mycket lätt vikt|Hakan mot bröstet|Långsamt åt båda hållen",
        ),
        "Neck Extension" to t(
            "Lie face down on a bench with the head over the edge and a light plate on the back of the head. Raise the head by looking up, then lower slowly.",
            "Ligg på mage på en bänk med huvudet över kanten och en lätt viktskiva på bakhuvudet. Lyft huvudet genom att titta upp och sänk långsamt.",
            "Very light weight|Look up to lift|Slow both ways",
            "Mycket lätt vikt|Titta upp för att lyfta|Långsamt åt båda hållen",
        ),
    )
}
