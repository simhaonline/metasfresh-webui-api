package de.metas.ui.web.payment_allocation.process;

import com.google.common.collect.ImmutableSet;
import de.metas.banking.payment.paymentallocation.PaymentAllocationRepository;
import de.metas.banking.payment.paymentallocation.PaymentToAllocateQuery;
import de.metas.bpartner.BPartnerId;
import de.metas.payment.PaymentId;
import de.metas.process.IProcessPrecondition;
import de.metas.process.ProcessExecutionResult.ViewOpenTarget;
import de.metas.process.ProcessExecutionResult.WebuiViewToOpen;
import de.metas.process.ProcessPreconditionsResolution;
import de.metas.ui.web.payment_allocation.PaymentsViewFactory;
import de.metas.ui.web.process.adprocess.ViewBasedProcessTemplate;
import de.metas.ui.web.view.CreateViewRequest;
import de.metas.ui.web.view.IViewsRepository;
import de.metas.ui.web.view.ViewId;
import de.metas.ui.web.window.datatypes.DocumentIdsSelection;
import de.metas.util.time.SystemTime;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.SpringContextHolder;

import java.util.Optional;

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

public class PaymentView_Launcher_From_BPartnerView extends ViewBasedProcessTemplate implements IProcessPrecondition
{
	private final PaymentAllocationRepository paymentAllocationRepo = SpringContextHolder.instance.getBean(PaymentAllocationRepository.class);
	private final IViewsRepository viewsFactory = SpringContextHolder.instance.getBean(IViewsRepository.class);

	@Override
	protected ProcessPreconditionsResolution checkPreconditionsApplicable()
	{
		if (!getSingleSelectedBPartnerId().isPresent())
		{
			return ProcessPreconditionsResolution.rejectWithInternalReason("not a single selected BPartner");
		}

		return ProcessPreconditionsResolution.accept();
	}

	@Override
	protected String doIt()
	{
		final ImmutableSet<PaymentId> paymentIds = retrievePaymentIds();
		if (paymentIds.isEmpty())
		{
			throw new AdempiereException("@NoOpenPayments@")
					.markAsUserValidationError();
		}

		final ViewId viewId = viewsFactory.createView(CreateViewRequest.builder(PaymentsViewFactory.WINDOW_ID)
				.setFilterOnlyIds(PaymentId.toIntSet(paymentIds))
				.build())
				.getViewId();

		getResult().setWebuiViewToOpen(WebuiViewToOpen.builder()
				.viewId(viewId.getViewId())
				.target(ViewOpenTarget.ModalOverlay)
				.build());

		return MSG_OK;
	}

	private ImmutableSet<PaymentId> retrievePaymentIds()
	{
		return paymentAllocationRepo.retrievePaymentIdsToAllocate(PaymentToAllocateQuery.builder()
				.evaluationDate(SystemTime.asZonedDateTime())
				.bpartnerId(getSingleSelectedBPartnerId().get())
				.build());
	}

	private Optional<BPartnerId> getSingleSelectedBPartnerId()
	{
		final DocumentIdsSelection selectedRowIds = getSelectedRowIds();
		return selectedRowIds.isSingleDocumentId()
				? Optional.of(selectedRowIds.getSingleDocumentId().toId(BPartnerId::ofRepoId))
				: Optional.empty();
	}
}
