package de.metas.ui.web.order.products_proposal.view;

import java.util.Set;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.lang.impl.TableRecordReference;
import org.compiere.model.I_C_BPartner;

import de.metas.bpartner.BPartnerId;
import de.metas.bpartner.service.IBPartnerDAO;
import de.metas.i18n.ITranslatableString;
import de.metas.pricing.PriceListId;
import de.metas.pricing.PricingSystemId;
import de.metas.pricing.service.IPriceListDAO;
import de.metas.process.IADProcessDAO;
import de.metas.ui.web.order.products_proposal.process.WEBUI_BPartner_ProductsProposal_Launcher;
import de.metas.ui.web.view.ViewCloseAction;
import de.metas.ui.web.view.ViewFactory;
import de.metas.ui.web.view.ViewId;
import de.metas.ui.web.view.descriptor.ViewLayout;
import de.metas.ui.web.window.datatypes.WindowId;
import de.metas.util.Services;
import de.metas.util.time.SystemTime;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2019 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

@ViewFactory(windowId = BPartnerProductsProposalViewFactory.WINDOW_ID_STRING)
public class BPartnerProductsProposalViewFactory extends ProductsProposalViewFactoryTemplate
{
	public static final String WINDOW_ID_STRING = "bpartnerProductsProposal";
	public static final WindowId WINDOW_ID = WindowId.fromJson(WINDOW_ID_STRING);

	protected BPartnerProductsProposalViewFactory()
	{
		super(WINDOW_ID);
	}

	@Override
	protected ViewLayout createViewLayout(ViewLayoutKey key)
	{
		final ITranslatableString caption = Services.get(IADProcessDAO.class)
				.retrieveProcessNameByClassIfUnique(WEBUI_BPartner_ProductsProposal_Launcher.class)
				.orElse(null);

		return ViewLayout.builder()
				.setWindowId(key.getWindowId())
				.setCaption(caption)
				.addElementsFromViewRowClass(ProductsProposalRow.class, key.getViewDataType())
				.removeElementByFieldName(ProductsProposalRow.FIELD_Qty)
				.allowViewCloseAction(ViewCloseAction.DONE)
				.build();
	}

	@Override
	protected ProductsProposalRowsLoader createRowsLoaderFromRecord(TableRecordReference recordRef)
	{
		final IBPartnerDAO bpartnersRepo = Services.get(IBPartnerDAO.class);
		final IPriceListDAO priceListsRepo = Services.get(IPriceListDAO.class);

		recordRef.assertTableName(I_C_BPartner.Table_Name);
		final BPartnerId bpartnerId = BPartnerId.ofRepoId(recordRef.getRecord_ID());
		I_C_BPartner bpartnerRecord = bpartnersRepo.getById(bpartnerId);

		final PricingSystemId pricingSystemId = extractPricingSystemId(bpartnerRecord);
		final Set<PriceListId> priceListIds = priceListsRepo.retrievePriceListIds(pricingSystemId);

		return ProductsProposalRowsLoader.builder()
				.priceListIds(priceListIds)
				.date(SystemTime.asLocalDate())
				.build();
	}

	private PricingSystemId extractPricingSystemId(I_C_BPartner bpartnerRecord)
	{
		PricingSystemId pricingSystemId = null;
		if (bpartnerRecord.isCustomer())
		{
			pricingSystemId = PricingSystemId.ofRepoIdOrNull(bpartnerRecord.getM_PricingSystem_ID());
		}
		if (pricingSystemId == null && bpartnerRecord.isVendor())
		{
			pricingSystemId = PricingSystemId.ofRepoIdOrNull(bpartnerRecord.getPO_PricingSystem_ID());
		}
		if (pricingSystemId == null)
		{
			throw new AdempiereException("@NotFound@ @M_PricingSystem_ID@");
		}
		return pricingSystemId;
	}

	@Override
	protected void beforeViewClose(ViewId viewId, ViewCloseAction closeAction)
	{
		// TODO Auto-generated method stub

	}

}
