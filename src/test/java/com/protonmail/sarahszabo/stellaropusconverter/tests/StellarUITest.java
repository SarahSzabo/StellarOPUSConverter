/*
 * Copyright (C) 2018 Sarah Szabo <SarahSzabo@Protonmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.protonmail.sarahszabo.stellaropusconverter.tests;

import com.protonmail.sarahszabo.stellaropusconverter.StellarUI;
import org.junit.Assert;

/**
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class StellarUITest {

    public StellarUITest() {
    }

    //@Test
    public void testConfirmationDIalog() {
        Assert.assertTrue(StellarUI.showConfirmationDialog("Choose Something"));
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
}
