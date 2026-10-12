import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ArrowLeft, Save, Send } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { PageHeader } from "@/components/layout";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { FileDropzone } from "@/features/documents/file-dropzone";
import { claimsService } from "@/services/claims";
import { createClaimSchema, type CreateClaimFormValues } from "@/schemas/claim-schema";
import { ApiRequestError } from "@/lib/api-client";
import type { ClaimDocument, DocumentType } from "@/types/document";

export function NewClaimPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [cancelOpen, setCancelOpen] = useState(false);
  const [createdClaimId, setCreatedClaimId] = useState<string | null>(null);
  const [pendingFiles, setPendingFiles] = useState<Array<{ file: File; documentType: DocumentType }>>([]);

  const form = useForm<CreateClaimFormValues>({
    resolver: zodResolver(createClaimSchema),
    defaultValues: {
      policyNumber: "",
      claimantName: "",
      claimantPhone: "",
      claimantEmail: "",
      vehicleRegistration: "",
      incidentDate: "",
      incidentTime: "",
      incidentLocation: "",
      incidentDescription: "",
      thirdPartyInvolved: false,
      policeReportNumber: "",
      injuriesReported: false,
      estimatedAmount: undefined,
    },
  });

  const createMutation = useMutation({
    mutationFn: (values: CreateClaimFormValues) =>
      claimsService.create({
        policyNumber: values.policyNumber,
        claimantName: values.claimantName,
        claimantPhone: values.claimantPhone,
        claimantEmail: values.claimantEmail,
        vehicleRegistration: values.vehicleRegistration,
        incidentDate: values.incidentDate,
        incidentTime: values.incidentTime || undefined,
        incidentLocation: values.incidentLocation,
        incidentDescription: values.incidentDescription,
        thirdPartyInvolved: values.thirdPartyInvolved,
        policeReportNumber: values.policeReportNumber || undefined,
        injuriesReported: values.injuriesReported,
        estimatedAmount: values.estimatedAmount,
        channel: "WEB_PORTAL",
      }),
    onSuccess: (claim) => {
      queryClient.invalidateQueries({ queryKey: ["claims"] });
      toast.success(`Claim ${claim.claimNumber} created`, {
        description: pendingFiles.length > 0
          ? `Uploading ${pendingFiles.length} document${pendingFiles.length === 1 ? "" : "s"}...`
          : "You can upload documents from the claim page.",
      });
      setCreatedClaimId(claim.id);
      navigate(`/app/claims/${claim.id}?tab=documents`, { replace: true });
    },
    onError: (error) => {
      if (error instanceof ApiRequestError && error.fieldErrors?.length) {
        for (const fe of error.fieldErrors) {
          form.setError(fe.field as keyof CreateClaimFormValues, { message: fe.message });
        }
        toast.error("Some fields need attention", { description: "Please review the highlighted fields." });
      } else {
        toast.error("Could not create claim", {
          description: error instanceof Error ? error.message : "Unexpected error",
        });
      }
    },
  });

  const onSubmit = form.handleSubmit((values) => createMutation.mutate(values));

  const handleCancel = () => {
    const dirty = form.formState.isDirty;
    if (dirty || pendingFiles.length > 0) setCancelOpen(true);
    else navigate(-1);
  };

  const isSubmitting = createMutation.isPending;

  return (
    <>
      <PageHeader
        title="New claim"
        description="Create a claim. You can upload supporting documents immediately after."
        actions={
          <Button variant="ghost" onClick={handleCancel} disabled={isSubmitting}>
            <ArrowLeft /> Cancel
          </Button>
        }
      />

      {createdClaimId && (
        <Alert variant="success" className="mb-4">
          <AlertTitle>Claim created</AlertTitle>
          <AlertDescription>
            Documents can now be uploaded. <Link to={`/app/claims/${createdClaimId}`} className="underline">Open claim</Link>.
          </AlertDescription>
        </Alert>
      )}

      <Form {...form}>
        <form onSubmit={onSubmit} noValidate className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Policy & claimant</CardTitle>
              <CardDescription>Policy details as stated by the claimant.</CardDescription>
            </CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-2">
              <FormField
                control={form.control}
                name="policyNumber"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Policy number</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="POL-MT-100001" autoComplete="off" disabled={isSubmitting} />
                    </FormControl>
                    <FormDescription>Format: POL-MT-######.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="vehicleRegistration"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Vehicle registration</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="ABC 123" disabled={isSubmitting} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="claimantName"
                render={({ field }) => (
                  <FormItem className="sm:col-span-2">
                    <FormLabel>Claimant name</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="Full name" autoComplete="name" disabled={isSubmitting} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="claimantPhone"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Claimant phone</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="+35679123456" autoComplete="tel" disabled={isSubmitting} />
                    </FormControl>
                    <FormDescription>E.164 format (e.g. +35679123456).</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="claimantEmail"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Claimant email</FormLabel>
                    <FormControl>
                      <Input {...field} type="email" placeholder="name@example.com" autoComplete="email" disabled={isSubmitting} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Incident</CardTitle>
              <CardDescription>What happened, where, and when.</CardDescription>
            </CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-2">
              <FormField
                control={form.control}
                name="incidentDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Incident date</FormLabel>
                    <FormControl>
                      <Input {...field} type="date" disabled={isSubmitting} />
                    </FormControl>
                    <FormDescription>Cannot be in the future.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="incidentTime"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Incident time (optional)</FormLabel>
                    <FormControl>
                      <Input {...field} type="time" disabled={isSubmitting} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="incidentLocation"
                render={({ field }) => (
                  <FormItem className="sm:col-span-2">
                    <FormLabel>Incident location</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="Street, locality" disabled={isSubmitting} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="incidentDescription"
                render={({ field }) => (
                  <FormItem className="sm:col-span-2">
                    <FormLabel>Incident description</FormLabel>
                    <FormControl>
                      <Textarea
                        {...field}
                        rows={5}
                        placeholder="Describe what happened in your own words (20–5000 characters)."
                        disabled={isSubmitting}
                      />
                    </FormControl>
                    <FormDescription>{field.value.length}/5000 characters</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Additional details</CardTitle>
              <CardDescription>Mark all that apply.</CardDescription>
            </CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-2">
              <FormField
                control={form.control}
                name="thirdPartyInvolved"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-center gap-3 rounded-md border p-3">
                    <FormControl>
                      <Checkbox
                        checked={field.value}
                        onCheckedChange={(v) => field.onChange(Boolean(v))}
                        disabled={isSubmitting}
                      />
                    </FormControl>
                    <div className="space-y-0.5">
                      <FormLabel className="text-sm">Third party involved</FormLabel>
                      <FormDescription>Another vehicle or party.</FormDescription>
                    </div>
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="injuriesReported"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-center gap-3 rounded-md border p-3">
                    <FormControl>
                      <Checkbox
                        checked={field.value}
                        onCheckedChange={(v) => field.onChange(Boolean(v))}
                        disabled={isSubmitting}
                      />
                    </FormControl>
                    <div className="space-y-0.5">
                      <FormLabel className="text-sm">Injuries reported</FormLabel>
                      <FormDescription>Anyone injured as a result.</FormDescription>
                    </div>
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="policeReportNumber"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Police report number (optional)</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="If a police report was filed" disabled={isSubmitting} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="estimatedAmount"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Estimated amount (optional)</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        min={0}
                        step="0.01"
                        inputMode="decimal"
                        value={field.value ?? ""}
                        onChange={(e) => {
                          const v = e.target.value;
                          field.onChange(v === "" ? undefined : Number(v));
                        }}
                        disabled={isSubmitting}
                        placeholder="0.00"
                      />
                    </FormControl>
                    <FormDescription>EUR — at most two decimals.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Supporting documents</CardTitle>
              <CardDescription>
                Optional at creation. Documents are uploaded on the claim detail page after the claim exists.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <FileDropzone
                disabled={isSubmitting}
                onFilesChange={(items) => setPendingFiles(items)}
              />
            </CardContent>
          </Card>

          <div className="sticky bottom-4 z-10 flex flex-col-reverse items-stretch gap-3 rounded-lg border bg-background/95 p-4 shadow-lg backdrop-blur sm:flex-row sm:items-center sm:justify-end">
            <p className="flex-1 text-xs text-muted-foreground">
              {pendingFiles.length > 0
                ? `${pendingFiles.length} file${pendingFiles.length === 1 ? "" : "s"} queued — uploaded after creation.`
                : "The AI never approves, rejects, or pays claims."}
            </p>
            <Button type="button" variant="outline" onClick={handleCancel} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? (
                <>Submitting…</>
              ) : (
                <>
                  <Send /> Submit claim
                </>
              )}
            </Button>
          </div>
        </form>
      </Form>

      <ConfirmDialog
        open={cancelOpen}
        onOpenChange={setCancelOpen}
        title="Discard this draft?"
        description="Any information you have entered will be lost. This cannot be undone."
        confirmLabel="Discard"
        cancelLabel="Keep editing"
        destructive
        onConfirm={() => {
          setCancelOpen(false);
          navigate(-1);
        }}
      />
    </>
  );
}