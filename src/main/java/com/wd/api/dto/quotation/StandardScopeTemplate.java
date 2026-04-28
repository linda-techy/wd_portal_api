package com.wd.api.dto.quotation;

import java.util.List;

/**
 * One row of Walldot's standard customer-facing scope-of-work table —
 * the 16 categories from the actual paper quotation (Excavation,
 * Foundation, Plinth Beam, …, Hand Rails). Used to seed a fresh
 * SQFT_RATE quotation so staff don't retype the descriptive content
 * for every customer.
 *
 * @param itemNumber  display order (1..16)
 * @param particulars short label rendered in the "Particulars" column
 *                    (e.g. "Foundation", "Plinth Beam")
 * @param description rich specification rendered in the "Item Description"
 *                    column — multi-line, can include brand specs and
 *                    max-cost ceilings ("Max. cost Rs. X/-")
 */
public record StandardScopeTemplate(
        int itemNumber,
        String particulars,
        String description
) {
    /**
     * The complete 16-row Walldot scope library, content lifted verbatim
     * from the live customer document ("Work Quotation for Mr Clinton
     * Amballur"). Stable order — the {@code itemNumber} matches the row
     * number on the paper template so staff can compare side-by-side.
     *
     * <p>Hardcoded as a Java constant rather than DB-seeded because
     * (a) the list is stable and rarely changes, (b) admin UI to edit
     * these isn't yet wired, and (c) keeping them in code lets a
     * deployment ship updated content without a data migration. When
     * admin-managed templates are needed, migrate to a
     * {@code quotation_scope_template} table mirroring the DPC pattern.
     */
    public static final List<StandardScopeTemplate> WALLDOT_DEFAULTS = List.of(
            new StandardScopeTemplate(1, "Excavation",
                    "Manually setting out the lines for excavation for foundation. "
                            + "Excavation using Hitachi/JCB."),
            new StandardScopeTemplate(2, "Foundation",
                    "Dry rubble masonry in dry packing and excavated earth filling "
                            + "for foundation (60cm × 60cm)."),
            new StandardScopeTemplate(3, "Basement",
                    "Random rubble masonry for basement outer pointing with cement "
                            + "mortar 1:8 (45cm × 35cm)."),
            new StandardScopeTemplate(4, "Plinth Beam",
                    "RCC 1:1.5:3 for belt above basement (30cm × 10cm thick)."),
            new StandardScopeTemplate(5, "Consolidation",
                    "Earth filling in basement and consolidation. (The material "
                            + "cost for filling the basement will be extra.)"),
            new StandardScopeTemplate(6, "Wall",
                    "Superstructure cement brick masonry in cement mortar 1:6."),
            new StandardScopeTemplate(7, "RCC Works",
                    "- RCC 1:1.5:3 for Lintels in Ground Floor.\n"
                            + "- RCC 1:1.5:3 for Sloped Roof Slab with Sunshade 10cm thickness.\n"
                            + "- RCC 1:1.5:3 for kitchen slab.\n"
                            + "Cement: Dalmia, Ambuja, ACC, Ramco.\n"
                            + "Steel: Kairaly, Kalliyath, Prince, Bharathi."),
            new StandardScopeTemplate(8, "Plastering",
                    "- Concrete surfaces plastering in cement mortar 1:4.\n"
                            + "- Inside wall plastering in cement mortar 1:6.\n"
                            + "- Outside with cement mortar 1:5."),
            new StandardScopeTemplate(9, "Doors and Windows",
                    "- All door and window frames are Steel.\n"
                            + "- Front door frame and shutter with TATA Pravesh steel door.\n"
                            + "- All interior door shutters are with nuwud (skin door) panelled.\n"
                            + "- All window shutters are steel with 3mm glass.\n"
                            + "- Bathroom doors are with PVC."),
            new StandardScopeTemplate(10, "Painting works",
                    "- Enamel paint finish for all door frames and windows (except front door).\n"
                            + "- One coat white wash on interior & exterior walls (JK / Asian).\n"
                            + "- Two-coat putty work on interior and exterior walls (Birla / Asian).\n"
                            + "- One coat cement primer on interior & exterior wall (Asian).\n"
                            + "- Two-coat emulsion painting on interior and exterior walls (Asian / Berger)."),
            new StandardScopeTemplate(11, "Flooring",
                    "- PCC 1:4:8 for flooring using 20mm aggregates.\n"
                            + "- Entire flooring works with vitrified tiles of maximum cost ₹50/- per sq.ft.\n"
                            + "- Bathroom floors with ceramic tiles of maximum cost ₹40/- per sq.ft.\n"
                            + "- Dadoing toilet wall with ceramic glazed wall tiles up to 7' height (max cost ₹40/-).\n"
                            + "- Laying black granite slab over the kitchen countertops, sit-out and moulding etc. (max cost ₹160/-)."),
            new StandardScopeTemplate(12, "Sanitary Fitting",
                    "- Steel single-bowl sink in kitchen (max cost ₹2,000/-) with sink tap (max cost ₹1,500/-).\n"
                            + "- Fittings in toilet (ISI Marked).\n"
                            + "- Orissa pan / EWC – white colour single piece (max cost ₹7,000/-).\n"
                            + "- Wash basin in dining room (max cost ₹2,000/-) with long body tap (max cost ₹1,500/-).\n"
                            + "- Wash basin in all toilets (max cost ₹1,500/-) with normal tap (max cost ₹1,000/-).\n"
                            + "- Health faucet × 1 (max cost ₹800/-).\n"
                            + "- Long body tap × 2 (one for hot water).\n"
                            + "- Overhead shower.\n"
                            + "- 22\" wash basin (light coloured) in dining room and bathroom."),
            new StandardScopeTemplate(13, "Electrical and Plumbing",
                    "- Two light points, one fan point, one foot lamp in each room.\n"
                            + "- Two 15-Amp plugs in kitchen and one in work area.\n"
                            + "- Two 5-Amp plugs and one 15-Amp plug in each room.\n"
                            + "- TV point in living room (5-Amp plug × 3).\n"
                            + "- Exhaust fan point in all bathrooms.\n"
                            + "- ISI Marked switches and electrical wire.\n"
                            + "- Single-phase ELCB and MCB.\n"
                            + "- PVC water tank 1,000 litres (ISO 3-layer PVC tank).\n"
                            + "Switches & Electricals: V-Guard, Polycab, GM.\n"
                            + "Plumbing: ISI Certified pipe."),
            new StandardScopeTemplate(14, "Ferro-cement works",
                    "- Ferro cement slab work for kitchen top, bottom cabinet and wardrobes.\n"
                            + "- Septic tank & soak pit with Ferro cement (2,000 L)."),
            new StandardScopeTemplate(15, "Water proofing",
                    "Waterproofing in toilets."),
            new StandardScopeTemplate(16, "Hand Rails",
                    "Providing SS hand rails for staircase.")
    );
}
