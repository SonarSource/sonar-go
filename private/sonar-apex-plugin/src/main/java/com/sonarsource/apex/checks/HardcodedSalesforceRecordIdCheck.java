/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.checks;

import com.sonarsource.apex.checks.utils.ExpressionUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.StringLiteralTree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;

@Rule(key = "S5379")
public class HardcodedSalesforceRecordIdCheck implements SlangCheck {

  private static final int SHORT_RECORD_ID_LENGTH = 15;
  private static final int LONG_RECORD_ID_LENGTH = 18;

  private static final Pattern RECORD_ID_PATTERN = Pattern.compile("[A-Za-z0-9]+");
  private static final Pattern RECORD_ID_THREE_CHAR_PREFIX_PATTERN = Pattern.compile("[aemz][0-9]{2}");

  private static final List<String> RECORD_ID_OTHERS_PREFIXES = Arrays.asList("kA", "ka", "CF00N", "0.00E");

  /**
   * Uppercase list of Salesforce Record ID prefixes comes from
   * http://www.fishofprey.com/2011/09/obscure-salesforce-object-key-prefixes.html
   * Prefixes are compared in a case-insensitive manner because:
   * https://help.salesforce.com/articleView?id=000324087&language=en_US&type=1&mode=1
   */
  private static final Set<String> RECORD_ID_THREE_CHAR_PREFIXES = new HashSet<>(Arrays.asList(
    "000", "00O", "00P", "00Q", "00R", "00S", "00T", "00U", "00V", "00W", "00X", "00Y", "00Z", "010", "011", "012",
    "013", "014", "015", "016", "017", "018", "019", "01A", "01B", "01C", "01D", "01E", "01F", "01G", "01H", "01I",
    "01J", "01K", "01L", "01M", "01N", "01O", "01P", "01Q", "01R", "01S", "01T", "01U", "01V", "01W", "01X", "01Y",
    "01Z", "020", "021", "022", "023", "024", "025", "026", "027", "028", "029", "02A", "02B", "02C", "02D", "02E",
    "02F", "02G", "02H", "02I", "02J", "02K", "02L", "02M", "02N", "02O", "02P", "02Q", "02R", "02S", "02T", "02U",
    "02V", "02W", "02X", "02Y", "02Z", "030", "031", "032", "033", "034", "035", "036", "037", "038", "039", "03H",
    "03M", "03A", "03B", "03C", "03D", "03E", "03F", "03G", "03I", "03J", "03K", "03N", "03Q", "03S", "03U", "040",
    "041", "042", "043", "044", "045", "04A", "04B", "04C", "04D", "04E", "04F", "04G", "04H", "04I", "04J", "04K",
    "04L", "04M", "04N", "04O", "04P", "04Q", "04R", "04S", "04T", "04U", "04V", "04W", "04X", "04Y", "04Z", "050",
    "051", "052", "053", "054", "055", "056", "057", "058", "059", "05B", "05G", "05H", "05I", "05J", "05L", "05M",
    "05N", "05P", "05Q", "05R", "05S", "05U", "05V", "05W", "05X", "05Z", "05A", "05C", "05D", "05E", "05F", "05K",
    "05T", "060", "061", "062", "063", "064", "065", "066", "067", "068", "069", "06A", "06F", "06G", "06N", "06O",
    "06P", "06Y", "06B", "06D", "06E", "06H", "06I", "06J", "06S", "06T", "070", "071", "072", "073", "074", "075",
    "076", "077", "078", "079", "07A", "07D", "07F", "07H", "07I", "07K", "07M", "07R", "07S", "07T", "07E", "07G",
    "07J", "07L", "07N", "07O", "07P", "07U", "07V", "07W", "07X", "07Y", "07Z", "080", "081", "082", "083", "084",
    "085", "086", "087", "08C", "08F", "08H", "08I", "08K", "08M", "08O", "08P", "08R", "08U", "08V", "08W", "08X",
    "08A", "08D", "08E", "08G", "08S", "090", "091", "092", "093", "094", "095", "096", "097", "099", "09B", "09C",
    "09I", "09L", "09M", "09N", "09O", "09P", "09S", "09T", "09U", "09V", "09W", "09X", "09Y", "09Z", "09A", "09D",
    "09E", "09F", "09G", "09H", "09J", "09K", "0A0", "0A1", "0A3", "0A4", "0A5", "0A7", "0A8", "0A9", "0AH", "0AA",
    "0AF", "0AG", "0AI", "0AJ", "0AK", "0AL", "0AN", "0AO", "0AP", "0AQ", "0AR", "0AS", "0AT", "0AU", "0AV", "0AW",
    "0AX", "0AY", "0AZ", "0B0", "0B1", "0B2", "0B3", "0B9", "0BG", "0BH", "0BJ", "0BR", "0BZ", "0BA", "0BB", "0BC",
    "0BD", "0BE", "0BF", "0BI", "0BK", "0BL", "0BM", "0BN", "0BO", "0BP", "0BS", "0BT", "0BU", "0BV", "0BW", "0BX",
    "0BY", "0C2", "0C3", "0C8", "0CC", "0CE", "0CF", "0CJ", "0CK", "0CL", "0CM", "0CO", "0CP", "0CQ", "0CU", "0CW",
    "0CX", "0CG", "0CI", "0CY", "0CZ", "0D0", "0D1", "0D2", "0D3", "0D4", "0D5", "0D6", "0D7", "0D8", "0D9", "0DA",
    "0DC", "0DM", "0DN", "0DS", "0DT", "0DU", "0DV", "0DX", "0DY", "0DB", "0DD", "0DE", "0DF", "0DG", "0DH", "0DI",
    "0DJ", "0DK", "0DL", "0DQ", "0E0", "0E2", "0E3", "0E4", "0E5", "0E6", "0E8", "0EA", "0ED", "0EI", "0EJ", "0EW",
    "0EE", "0EF", "0EG", "0EL", "0EM", "0ER", "0ET", "0EV", "0EX", "0EY", "0EZ", "0F0", "0F1", "0F2", "0F3", "0F5",
    "0F7", "0F8", "0F9", "0FB", "0FM", "0FX", "0FA", "0FE", "0FF", "0FG", "0FH", "0FI", "0FJ", "0FL", "0FO", "0FP",
    "0FQ", "0FS", "0FT", "0FU", "0FV", "0FY", "0FZ", "0G1", "0G2", "0G4", "0G5", "0G8", "0G9", "0GD", "0GE", "0GH",
    "0GI", "0GJ", "0GK", "0GL", "0GM", "0GN", "0GP", "0GS", "0GT", "0GU", "0GW", "0GY", "0GA", "0GC", "0H0", "0H1",
    "0H2", "0H4", "0H6", "0H7", "0H9", "0HN", "0HO", "0HP", "0HQ", "0HR", "0HU", "0HV", "0HW", "0HX", "0HY", "0HZ",
    "0HA", "0HE", "0HF", "0HG", "0HI", "0HJ", "0HK", "0HL", "0HS", "0I0", "0I1", "0I2", "0I3", "0I4", "0I5", "0I6",
    "0I7", "0I8", "0I9", "0IC", "0ID", "0IF", "0IG", "0IS", "0IX", "0IY", "0IA", "0IB", "0IH", "0II", "0IJ", "0IK",
    "0IL", "0IO", "0IT", "0IU", "0IV", "0IW", "0IZ", "0J0", "0J2", "0J4", "0J5", "0J8", "0JW", "0JX", "0JA", "0JB",
    "0JD", "0JE", "0JF", "0JG", "0JI", "0JJ", "0JK", "0JL", "0JM", "0JN", "0JO", "0JP", "0JQ", "0JR", "0JS", "0JT",
    "0JU", "0JV", "0JY", "0JZ", "0K0", "0K2", "0K3", "0K4", "0K6", "0K7", "0K9", "0KY", "0KB", "0KC", "0KD", "0KE",
    "0KG", "0KH", "0KI", "0KM", "0KN", "0KO", "0KP", "0KQ", "0KR", "0KS", "0KT", "0KU", "0KZ", "0L2", "0L3", "0L4",
    "0L5", "0LH", "0LN", "0LV", "0LC", "0LD", "0LE", "0LF", "0LG", "0LI", "0LJ", "0LM", "0LO", "0LQ", "0LS", "0LU",
    "0LW", "0LX", "0LY", "0M0", "0M1", "0M2", "0M3", "0M4", "0M5", "0M6", "0M9", "0MD", "0ME", "0MH", "0MJ", "0MN",
    "0MO", "0MQ", "0MR", "0MV", "0MW", "0MA", "0MB", "0MF", "0MG", "0MI", "0MK", "0MP", "0MS", "0MU", "0MY", "0MZ",
    "0N0", "0N1", "0N2", "0N3", "0N4", "0N5", "0N9", "0NB", "0NC", "0NK", "0NL", "0NM", "0NN", "0NQ", "0NR", "0NU",
    "0NX", "0NZ", "0NA", "0ND", "0NE", "0NF", "0NG", "0NH", "0NI", "0NJ", "0NO", "0NP", "0NT", "0NV", "0NW", "0O0",
    "0O1", "0O6", "0O7", "0O8", "0OC", "0OD", "0OG", "0OH", "0OL", "0OO", "0OP", "0OV", "0OZ", "0OA", "0OB", "0OE",
    "0OF", "0OI", "0OM", "0OQ", "0OR", "0P0", "0P1", "0P2", "0P9", "0PB", "0PC", "0PD", "0PF", "0PO", "0PA", "0PK",
    "0PL", "0PM", "0PP", "0PQ", "0PR", "0PS", "0PT", "0PU", "0PV", "0PX", "0PY", "0PZ", "0Q0", "0Q1", "0Q3", "0Q5",
    "0Q7", "0QL", "0QM", "0QR", "0QU", "0QV", "0QB", "0QC", "0QD", "0QG", "0QI", "0QJ", "0QK", "0QN", "0QO", "0QP",
    "0QT", "0QY", "0QZ", "0R0", "0R1", "0R2", "0R8", "0RA", "0RC", "0RE", "0RJ", "0RM", "0RY", "0RZ", "0RB", "0RD",
    "0RF", "0RG", "0RH", "0RI", "0RL", "0RR", "0RT", "0RU", "0RV", "0RX", "0S1", "0S2", "0SE", "0SL", "0SM", "0SO",
    "0ST", "0SU", "0SV", "0SK", "0SN", "0SY", "0T5", "0T6", "0TH", "0TI", "0TO", "0TY", "0TJ", "0TT", "0TV", "0TW",
    "0U5", "0UM", "0US", "0UT", "0UA", "0W0", "0W1", "0W2", "0W3", "0W4", "0W5", "0W7", "0W8", "0WA", "0WB", "0WC",
    "0WD", "0WE", "0WF", "0WG", "0WH", "0WI", "0WJ", "0WK", "0WL", "0WM", "0WO", "0XA", "0XB", "0XC", "0XD", "0XE",
    "0XH", "0XR", "0XU", "0XV", "0YA", "0YM", "0YQ", "0YS", "0YU", "0YW", "0ZA", "0ZQ", "0ZX", "0A2", "0AB", "0AD",
    "0AM", "0C0", "0C1", "0CA", "0CS", "0DR", "0E1", "0EH", "0EB", "0EN", "0EO", "0EP", "0EQ", "0FR", "0GV", "0HC",
    "0HD", "0HT", "0IN", "0KA", "0MT", "0NS", "0RP", "0RS", "0SA", "0SP", "0SR", "0T0", "0TA", "0TE", "0TG", "0TN",
    "0TR", "0TS", "0TU", "0UP", "0UR", "100", "101", "102", "10Y", "10Z", "110", "111", "112", "113", "11A", "130",
    "131", "149", "19I", "1AB", "1AR", "1CA", "1CC", "1CF", "1CP", "1CS", "1DS", "1ED", "1EF", "1ES", "1EV", "1EP",
    "1FS", "1HA", "1HB", "1HC", "1JS", "1L7", "1L8", "1LB", "1MA", "1MP", "1MC", "1NR", "1OZ", "1S1", "1SR", "1ST",
    "1SL", "1WK", "1WL", "1BM", "1BR", "1CB", "1CI", "1CL", "1CM", "1CR", "1DC", "1DE", "1DO", "1DP", "1DR", "1GH",
    "1GP", "1MR", "1O1", "1PM", "1PS", "1RP", "1RR", "1SA", "1TE", "1TS", "1VC", "200", "201", "202", "203", "204",
    "205", "208", "26Z", "2AS", "2CE", "2ED", "2EP", "2FE", "2FF", "2LA", "2SR", "2HF", "2ON", "300", "301", "307",
    "308", "309", "30L", "30Q", "30S", "30W", "30X", "30A", "30C", "30D", "30E", "30F", "30G", "30M", "30P", "30R",
    "30T", "30V", "310", "31A", "31S", "31C", "31D", "31I", "31O", "31V", "31W", "31X", "31Y", "31Z", "3DS", "3DB",
    "3DP", "3HP", "3J5", "3M0", "3M1", "3M2", "3M3", "3M4", "3M5", "3M6", "3MA", "3MB", "3MC", "3MD", "3ME", "3MF",
    "3MG", "3MH", "3MI", "3MJ", "3MM", "3MN", "3MO", "3MQ", "3MR", "3MS", "3MU", "3MV", "3MW", "3ML", "3MT", "3N1",
    "3NA", "3NC", "3NO", "3NS", "3NT", "3NU", "3NV", "3NW", "3NX", "3NY", "3NZ", "3PS", "3PX", "3PB", "3PH", "3PP",
    "3SP", "3SS", "3MK", "400", "401", "402", "403", "404", "405", "406", "407", "408", "410", "412", "413", "4A0",
    "4F0", "4F1", "4F2", "4F3", "4F4", "4F5", "4M5", "4M6", "4NA", "4NB", "4NC", "4ND", "4NW", "4WZ", "4CI", "4CL",
    "4CO", "4DT", "4FE", "4FP", "4FT", "4IE", "4PB", "4PV", "4SR", "4ST", "4SV", "4VE", "4WS", "4WT", "4XS", "500",
    "501", "550", "551", "552", "555", "557", "570", "571", "572", "573", "574", "5CS", "5PA", "5SP", "600", "601",
    "602", "604", "605", "606", "607", "608", "62C", "6AA", "6AB", "6AC", "6AD", "6EB", "6SS", "6PS", "700", "701",
    "707", "708", "709", "70A", "70B", "70C", "70D", "710", "711", "712", "713", "714", "715", "716", "729", "737",
    "750", "751", "752", "753", "754", "766", "777", "7EH", "7EQ", "7ER", "7DL", "7PV", "7TF", "7TG", "800", "801",
    "802", "803", "804", "805", "806", "807", "80D", "810", "811", "817", "820", "822", "823", "824", "825", "828",
    "829", "82B", "888", "889", "906", "907", "910", "911", "912", "9BV", "9DV", "9NV", "9YZ", "X00"));

  @Override
  public void initialize(InitContext init) {
    init.register(StringLiteralTree.class, (ctx, tree) -> {
      if (isNotInsideTestClass(ctx) && isSalesforceRecordId(tree.content())) {
        ctx.reportIssue(tree, "Replace this hardcoded record ID");
      }
    });
  }

  private static boolean isSalesforceRecordId(String text) {
    if (text.length() == SHORT_RECORD_ID_LENGTH || text.length() == LONG_RECORD_ID_LENGTH) {
      return isThreeCharSalesforceRecordId(text) ||
        RECORD_ID_OTHERS_PREFIXES.stream().anyMatch(text::startsWith);
    }
    return false;
  }

  private static boolean isThreeCharSalesforceRecordId(String text) {
    String threeCharPrefix = text.substring(0, 3);
    return (RECORD_ID_THREE_CHAR_PREFIXES.contains(threeCharPrefix.toUpperCase(Locale.ROOT)) ||
      RECORD_ID_THREE_CHAR_PREFIX_PATTERN.matcher(threeCharPrefix).matches()) &&
      RECORD_ID_PATTERN.matcher(text).matches();
  }

  private static boolean isNotInsideTestClass(CheckContext ctx) {
    return ctx.ancestors().stream()
      .filter(ClassDeclarationTree.class::isInstance)
      .map(ClassDeclarationTree.class::cast)
      .noneMatch(ExpressionUtils::isTestClass);
  }

}
