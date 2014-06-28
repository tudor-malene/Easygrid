package org.grails.plugin.easygrid

/**
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class FilterUtils {

    /*   static List opMapping = [
               [ILike, CN],
               [NotILike, NC],
               [Like, CN],
               [NotLike, NC],
               [Equal, EQ],
               [NotEqual, NE],
               [IsNull, null],
               [IsNotNull, null],
               [LessThan, LT],
               [LessThanEquals, LE],
               [GreaterThan, GT],
               [GreaterThanEquals, GE],
               [Between, null],
               [InList, IN],
               [NotInList, NI],
               [BeginsWith, BW],
               [IBeginsWith, BW],
               [EndsWith, EW],
               [IEndsWith, EW],
       ]

       static FilterPaneOperationType fp2eg(FilterOperatorsEnum op) {
           def val = opMapping.find { it[1] == op }
           if (val) {
               val[0]
           }
       }

       static FilterOperatorsEnum eg2fp(FilterPaneOperationType op) {
           def val = opMapping.find { it[0] == op }
           if (val) {
               val[1]
           }
       }
   */

    /**
     * code borrowed from the FilterPane plugin
     * @param opType
     * @return
     */
    static getOperatorMapKey(opType) {
        if (!opType) {
            return null
        }

        def type = 'text'
        if (opType.getSimpleName().equalsIgnoreCase("boolean")) {
            type = 'boolean'
        } else if (opType == Byte || opType == byte || opType == Integer || opType == int || opType == Long || opType == long
                || opType == Double || opType == double || opType == Float || opType == float
                || opType == Short || opType == short || opType == BigDecimal || opType == BigInteger) {
            type = 'numeric'
        } else if (Date.isAssignableFrom(opType) || isJodaDate(opType)) {
            type = 'date'
        } else if (opType.isEnum()) {
            type = 'enum'
        } else if (opType.simpleName.equalsIgnoreCase("currency")) {
            type = 'currency'
        } else if (opType == Class)
            type = 'class'
        type
    }

    static isJodaDate(type) {
        try {
            Class.forName('org.joda.time.base.AbstractInstant').isAssignableFrom(type) || Class.forName('org.joda.time.base.AbstractPartial').isAssignableFrom(type)
        } catch (ClassNotFoundException ex) {
            //no joda defined
            return false
        }
    }

}
